package com.hussain.assistantchooser.overlay

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.assistantchooser.core.AppCache
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.core.KEY_CUSTOM_APPS
import com.hussain.assistantchooser.core.KEY_OVERLAY_SOURCE
import com.hussain.assistantchooser.core.PREFS_NAME
import com.hussain.assistantchooser.core.OverlaySource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OverlayViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = getApplication<Application>()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _apps      = MutableStateFlow<List<AssistantApp>>(emptyList())
    val apps: StateFlow<List<AssistantApp>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _overlaySource = MutableStateFlow(OverlaySource.ASSISTANT_APPS)
    val overlaySource: StateFlow<OverlaySource> = _overlaySource.asStateFlow()

    private val _allApps = MutableStateFlow<List<AssistantApp>>(emptyList())
    val allApps: StateFlow<List<AssistantApp>> = _allApps.asStateFlow()

    private val _savedCustomPackages = MutableStateFlow<List<String>>(emptyList())
    val savedCustomPackages: StateFlow<List<String>> = _savedCustomPackages.asStateFlow()

    init {
        viewModelScope.launch {
            val ready = AppCache.state
                .filter { it.isReady }
                .first()

            val source             = OverlaySource.fromString(prefs.getString(KEY_OVERLAY_SOURCE, null))
            _overlaySource.value   = source
            _allApps.value         = ready.allApps
            _savedCustomPackages.value =
                (prefs.getStringSet(KEY_CUSTOM_APPS, emptySet()) ?: emptySet()).toList()
            _apps.value            = resolveApps(ready, source)
            _isLoading.value       = false
        }
    }

    fun saveCustomApps(packages: List<String>) {
        prefs.edit().putStringSet(KEY_CUSTOM_APPS, packages.toSet()).apply()
        _savedCustomPackages.value = packages
        val cache = AppCache.state.value
        if (cache.isReady) {
            _apps.value = cache.allApps.filter { it.packageName in packages }
        }
    }

    private fun resolveApps(cache: AppCache.CacheState, source: OverlaySource): List<AssistantApp> =
        when (source) {
            OverlaySource.ASSISTANT_APPS -> cache.assistantApps
            OverlaySource.CUSTOM_APPS   -> {
                val saved = prefs.getStringSet(KEY_CUSTOM_APPS, emptySet()) ?: emptySet()
                cache.allApps.filter { it.packageName in saved }
            }
        }
}
