package com.hussain.assistantchooser.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.assistantchooser.core.AppCache
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.core.KEY_CUSTOM_APPS
import com.hussain.assistantchooser.core.PREFS_NAME
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _assistantApps = MutableStateFlow<List<AssistantApp>>(emptyList())
    val assistantApps: StateFlow<List<AssistantApp>> = _assistantApps.asStateFlow()

    private val _allApps = MutableStateFlow<List<AssistantApp>>(emptyList())
    val allApps: StateFlow<List<AssistantApp>> = _allApps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _savedCustomApps = MutableStateFlow(
        prefs.getStringSet(KEY_CUSTOM_APPS, emptySet()) ?: emptySet()
    )
    val savedCustomApps: StateFlow<Set<String>> = _savedCustomApps.asStateFlow()

    init {
        AppCache.state
            .onEach { cache ->
                if (cache.isReady) {
                    _assistantApps.value = cache.assistantApps
                    _allApps.value       = cache.allApps
                    _isLoading.value     = false
                }
            }
            .launchIn(viewModelScope)
    }

    fun saveCustomApps(packages: List<String>) {
        val set = packages.toSet()
        prefs.edit().putStringSet(KEY_CUSTOM_APPS, set).apply()
        _savedCustomApps.value = set
    }
}
