package com.hussain.assistantchooser.settings

import android.content.Context
import com.hussain.assistantchooser.core.*
import org.json.JSONArray
import org.json.JSONObject

object BackupUtils {

    private const val BACKUP_IDENTIFIER = "assistant_chooser_backup_id"
    private const val APP_ID_VALUE = "com.hussain.assistantchooser"

    fun createExportJson(
        context: Context,
        exportCustomApps: Boolean,
        exportAppSettings: Boolean,
        exportGlobalSettings: Boolean
    ): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val root = JSONObject()

        // App identifier to prevent importing files from other apps
        root.put(BACKUP_IDENTIFIER, APP_ID_VALUE)

        if (exportCustomApps) {
            val customApps = prefs.getStringSet(KEY_CUSTOM_APPS, emptySet()) ?: emptySet()
            root.put(KEY_CUSTOM_APPS, JSONArray(customApps.toList()))
        }

        if (exportAppSettings) {
            val appSettings = JSONObject().apply {
                put(KEY_OPEN_APP, prefs.getBoolean(KEY_OPEN_APP, false))
                put(KEY_CLOSE_AFTER_LAUNCH, prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true))
                put(KEY_SHOW_PACKAGE_NAME, prefs.getBoolean(KEY_SHOW_PACKAGE_NAME, true))
                put(KEY_SHOW_APP_NAME, prefs.getBoolean(KEY_SHOW_APP_NAME, true))
            }
            root.put("app_settings", appSettings)
        }

        if (exportGlobalSettings) {
            val globalSettings = JSONObject().apply {
                put(KEY_APP_FILTER_MODE, prefs.getString(KEY_APP_FILTER_MODE, AppFilterMode.VOICE_ASSISTANTS.name))
                put(KEY_OVERLAY_SOURCE, prefs.getString(KEY_OVERLAY_SOURCE, OverlaySource.ASSISTANT_APPS.name))
                put(KEY_FIRST_LAUNCH, prefs.getBoolean(KEY_FIRST_LAUNCH, false))
            }
            root.put("global_settings", globalSettings)
        }

        return root.toString(4)
    }

    fun importFromJson(context: Context, jsonString: String): Boolean = runCatching {
        val root = JSONObject(jsonString)
        
        // Validation: Check if this file belongs to Assistant Chooser
        if (root.optString(BACKUP_IDENTIFIER) != APP_ID_VALUE) {
            return false
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val edit = prefs.edit()

        if (root.has(KEY_CUSTOM_APPS)) {
            val arr = root.getJSONArray(KEY_CUSTOM_APPS)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                set.add(arr.getString(i))
            }
            edit.putStringSet(KEY_CUSTOM_APPS, set)
        }

        if (root.has("app_settings")) {
            val appSettings = root.getJSONObject("app_settings")
            if (appSettings.has(KEY_OPEN_APP)) edit.putBoolean(KEY_OPEN_APP, appSettings.getBoolean(KEY_OPEN_APP))
            if (appSettings.has(KEY_CLOSE_AFTER_LAUNCH)) edit.putBoolean(KEY_CLOSE_AFTER_LAUNCH, appSettings.getBoolean(KEY_CLOSE_AFTER_LAUNCH))
            if (appSettings.has(KEY_SHOW_PACKAGE_NAME)) edit.putBoolean(KEY_SHOW_PACKAGE_NAME, appSettings.getBoolean(KEY_SHOW_PACKAGE_NAME))
            if (appSettings.has(KEY_SHOW_APP_NAME)) edit.putBoolean(KEY_SHOW_APP_NAME, appSettings.getBoolean(KEY_SHOW_APP_NAME))
        }

        if (root.has("global_settings")) {
            val globalSettings = root.getJSONObject("global_settings")
            if (globalSettings.has(KEY_APP_FILTER_MODE)) edit.putString(KEY_APP_FILTER_MODE, globalSettings.getString(KEY_APP_FILTER_MODE))
            if (globalSettings.has(KEY_OVERLAY_SOURCE)) edit.putString(KEY_OVERLAY_SOURCE, globalSettings.getString(KEY_OVERLAY_SOURCE))
            if (globalSettings.has(KEY_FIRST_LAUNCH)) edit.putBoolean(KEY_FIRST_LAUNCH, globalSettings.getBoolean(KEY_FIRST_LAUNCH))
        }

        edit.apply()
        true
    }.getOrDefault(false)
}
