package com.hussain.assistantchooser.overlay

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.assistantchooser.core.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OverlayViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = getApplication<Application>()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _overlaySource = MutableStateFlow(
        OverlaySource.fromString(prefs.getString(KEY_OVERLAY_SOURCE, null))
    )
    val overlaySource: StateFlow<OverlaySource> = _overlaySource.asStateFlow()

    private val _savedCustomPackages = MutableStateFlow(
        (prefs.getStringSet(KEY_CUSTOM_APPS, emptySet()) ?: emptySet()).toList()
    )
    val savedCustomPackages: StateFlow<List<String>> = _savedCustomPackages.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.fromString(prefs.getString(KEY_THEME_MODE, null))
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _showAppName = MutableStateFlow(prefs.getBoolean(KEY_SHOW_APP_NAME, true))
    val showAppName: StateFlow<Boolean> = _showAppName.asStateFlow()

    private val _openApp = MutableStateFlow(prefs.getBoolean(KEY_OPEN_APP, false))
    val openApp: StateFlow<Boolean> = _openApp.asStateFlow()

    private val _closeAfter = MutableStateFlow(prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true))
    val closeAfter: StateFlow<Boolean> = _closeAfter.asStateFlow()

    private val _themedIconMode = MutableStateFlow(
        ThemedIconMode.fromString(prefs.getString(KEY_THEMED_ICONS, null))
    )
    val themedIconMode: StateFlow<ThemedIconMode> = _themedIconMode.asStateFlow()

    // Fully reactive list of apps based on source and saved selection
    val apps: StateFlow<List<AssistantApp>> = combine(
        AppCache.state,
        _overlaySource,
        _savedCustomPackages
    ) { state, source, saved ->
        if (state.isReady) {
            _isLoading.value = false
            when (source) {
                OverlaySource.ASSISTANT_APPS -> state.assistantApps
                OverlaySource.CUSTOM_APPS    -> state.allApps.filter { it.packageName in saved }
            }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allApps: StateFlow<List<AssistantApp>> = AppCache.state
        .map { it.allApps }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            KEY_CUSTOM_APPS -> {
                _savedCustomPackages.value = (sharedPreferences.getStringSet(KEY_CUSTOM_APPS, emptySet()) ?: emptySet()).toList()
            }
            KEY_OVERLAY_SOURCE -> {
                _overlaySource.value = OverlaySource.fromString(sharedPreferences.getString(KEY_OVERLAY_SOURCE, null))
            }
            KEY_THEME_MODE -> {
                _themeMode.value = ThemeMode.fromString(sharedPreferences.getString(KEY_THEME_MODE, null))
            }
            KEY_SHOW_APP_NAME -> {
                _showAppName.value = sharedPreferences.getBoolean(KEY_SHOW_APP_NAME, true)
            }
            KEY_OPEN_APP -> {
                _openApp.value = sharedPreferences.getBoolean(KEY_OPEN_APP, false)
            }
            KEY_CLOSE_AFTER_LAUNCH -> {
                _closeAfter.value = sharedPreferences.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true)
            }
            KEY_THEMED_ICONS -> {
                _themedIconMode.value = ThemedIconMode.fromString(sharedPreferences.getString(KEY_THEMED_ICONS, null))
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun saveCustomApps(packages: List<String>) {
        prefs.edit().putStringSet(KEY_CUSTOM_APPS, packages.toSet()).apply()
    }
}
