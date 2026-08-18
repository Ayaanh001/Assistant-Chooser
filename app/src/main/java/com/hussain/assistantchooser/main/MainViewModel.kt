package com.hussain.assistantchooser.main

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.assistantchooser.BuildConfig
import com.hussain.assistantchooser.core.*
import com.hussain.assistantchooser.settings.GitHubRelease
import com.hussain.assistantchooser.settings.checkLatestVersionFromGitHub
import com.hussain.assistantchooser.settings.isNewerVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _savedCustomApps = MutableStateFlow(
        prefs.getStringSet(KEY_CUSTOM_APPS, emptySet()) ?: emptySet()
    )
    val savedCustomApps: StateFlow<Set<String>> = _savedCustomApps.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.fromString(prefs.getString(KEY_THEME_MODE, null))
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _showPackageName = MutableStateFlow(prefs.getBoolean(KEY_SHOW_PACKAGE_NAME, false))
    val showPackageName: StateFlow<Boolean> = _showPackageName.asStateFlow()

    private val _openApp = MutableStateFlow(prefs.getBoolean(KEY_OPEN_APP, false))
    val openApp: StateFlow<Boolean> = _openApp.asStateFlow()

    private val _closeAfterLaunch = MutableStateFlow(prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true))
    val closeAfterLaunch: StateFlow<Boolean> = _closeAfterLaunch.asStateFlow()

    private val _themedIconMode = MutableStateFlow(
        ThemedIconMode.fromString(prefs.getString(KEY_THEMED_ICONS, null))
    )
    val themedIconMode: StateFlow<ThemedIconMode> = _themedIconMode.asStateFlow()

    private val _appFilterMode = MutableStateFlow(
        AppFilterMode.valueOf(prefs.getString(KEY_APP_FILTER_MODE, AppFilterMode.VOICE_ASSISTANTS.name)
            ?: AppFilterMode.VOICE_ASSISTANTS.name)
    )
    val appFilterMode: StateFlow<AppFilterMode> = _appFilterMode.asStateFlow()

    val updateAvailable: StateFlow<GitHubRelease?> = AppCache.state
        .map { it.latestRelease }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val assistantApps: StateFlow<List<AssistantApp>> = AppCache.state
        .map { it.assistantApps }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allApps: StateFlow<List<AssistantApp>> = AppCache.state
        .map { it.allApps }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            KEY_CUSTOM_APPS -> _savedCustomApps.value = sharedPreferences.getStringSet(KEY_CUSTOM_APPS, emptySet()) ?: emptySet()
            KEY_THEME_MODE -> _themeMode.value = ThemeMode.fromString(sharedPreferences.getString(KEY_THEME_MODE, null))
            KEY_SHOW_PACKAGE_NAME -> _showPackageName.value = sharedPreferences.getBoolean(KEY_SHOW_PACKAGE_NAME, false)
            KEY_OPEN_APP -> _openApp.value = sharedPreferences.getBoolean(KEY_OPEN_APP, false)
            KEY_CLOSE_AFTER_LAUNCH -> _closeAfterLaunch.value = sharedPreferences.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true)
            KEY_THEMED_ICONS -> _themedIconMode.value = ThemedIconMode.fromString(sharedPreferences.getString(KEY_THEMED_ICONS, null))
            KEY_APP_FILTER_MODE -> {
                val modeStr = sharedPreferences.getString(KEY_APP_FILTER_MODE, AppFilterMode.VOICE_ASSISTANTS.name)
                _appFilterMode.value = AppFilterMode.valueOf(modeStr ?: AppFilterMode.VOICE_ASSISTANTS.name)
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        AppCache.state
            .onEach { if (it.isReady) _isLoading.value = false }
            .launchIn(viewModelScope)
        checkForUpdates()
    }

    private fun checkForUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            val latest = checkLatestVersionFromGitHub("Ayaanh001", "Assistant-Chooser")
            val current = BuildConfig.VERSION_NAME
            if (latest != null) {
                val tag = latest.tagName.removePrefix("v")
                if (isNewerVersion(tag, current)) {
                    AppCache.setLatestRelease(latest)
                } else {
                    // Current version is up-to-date or newer, clear any pending update state
                    AppCache.setLatestRelease(null)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun saveCustomApps(packages: List<String>) {
        prefs.edit().putStringSet(KEY_CUSTOM_APPS, packages.toSet()).apply()
    }

    fun setAppFilterMode(mode: AppFilterMode) {
        prefs.edit().putString(KEY_APP_FILTER_MODE, mode.name).apply()
    }
}
