package com.hussain.assistantchooser.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class CustomAppListManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _appLists = MutableStateFlow<List<CustomAppList>>(emptyList())
    val appLists: StateFlow<List<CustomAppList>> = _appLists.asStateFlow()

    private val _activeListId = MutableStateFlow<String?>(null)
    val activeListId: StateFlow<String?> = _activeListId.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_CUSTOM_APP_LISTS, KEY_ACTIVE_CUSTOM_LIST_ID -> load()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        migrateIfNeeded()
        load()
    }

    private fun migrateIfNeeded() {
        val listsStr = prefs.getString(KEY_CUSTOM_APP_LISTS, null)
        if (listsStr == null) {
            // Migrate old custom apps
            val oldApps = prefs.getStringSet(KEY_CUSTOM_APPS, emptySet()) ?: emptySet()
            val defaultList = CustomAppList(name = "Default", packages = oldApps.toList())
            saveLists(listOf(defaultList))
            setActiveListId(defaultList.id)
            prefs.edit().remove(KEY_CUSTOM_APPS).apply() // optional, clean up old
        }
    }

    private fun load() {
        val listsStr = prefs.getString(KEY_CUSTOM_APP_LISTS, null)
        val loadedLists = if (listsStr != null) {
            try {
                val array = JSONArray(listsStr)
                List(array.length()) { CustomAppList.fromJSONObject(array.getJSONObject(it)) }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        _appLists.value = loadedLists

        val activeId = prefs.getString(KEY_ACTIVE_CUSTOM_LIST_ID, null)
        _activeListId.value = activeId ?: loadedLists.firstOrNull()?.id
    }

    private fun saveLists(lists: List<CustomAppList>) {
        val array = JSONArray()
        lists.forEach { array.put(it.toJSONObject()) }
        prefs.edit().putString(KEY_CUSTOM_APP_LISTS, array.toString()).apply()
    }

    fun setActiveListId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_CUSTOM_LIST_ID, id).apply()
    }

    fun addList(name: String): CustomAppList {
        val newList = CustomAppList(name = name, packages = emptyList())
        val updated = _appLists.value + newList
        saveLists(updated)
        if (_activeListId.value == null) {
            setActiveListId(newList.id)
        }
        return newList
    }

    fun updateList(updatedList: CustomAppList) {
        val updated = _appLists.value.map { if (it.id == updatedList.id) updatedList else it }
        saveLists(updated)
    }

    fun deleteList(id: String) {
        val updated = _appLists.value.filter { it.id != id }
        saveLists(updated)
        if (_activeListId.value == id) {
            setActiveListId(updated.firstOrNull()?.id ?: "")
        }
    }

    fun replaceAll(lists: List<CustomAppList>, activeId: String?) {
        _appLists.value = lists
        if (activeId != null) {
            _activeListId.value = activeId
        }
        val array = JSONArray()
        lists.forEach { array.put(it.toJSONObject()) }
        val editor = prefs.edit().putString(KEY_CUSTOM_APP_LISTS, array.toString())
        if (activeId != null) {
            editor.putString(KEY_ACTIVE_CUSTOM_LIST_ID, activeId)
        }
        editor.apply()
    }
}
