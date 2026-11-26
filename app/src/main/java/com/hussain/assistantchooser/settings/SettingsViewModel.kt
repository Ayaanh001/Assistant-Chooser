package com.hussain.assistantchooser.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.assistantchooser.BuildConfig
import com.hussain.assistantchooser.core.AppCache
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.core.CustomAppList
import com.hussain.assistantchooser.core.CustomAppListManager
import com.hussain.assistantchooser.core.KEY_CLOSE_AFTER_LAUNCH
import com.hussain.assistantchooser.core.KEY_OPEN_APP
import com.hussain.assistantchooser.core.KEY_OVERLAY_HEADER_BUTTON
import com.hussain.assistantchooser.core.KEY_OVERLAY_HEADER_BUTTON_LONG_PRESS
import com.hussain.assistantchooser.core.KEY_OVERLAY_SOURCE
import com.hussain.assistantchooser.core.KEY_SHOW_APP_NAME
import com.hussain.assistantchooser.core.KEY_SHOW_OVERLAY_SWITCH
import com.hussain.assistantchooser.core.KEY_SHOW_PACKAGE_NAME
import com.hussain.assistantchooser.core.KEY_SWIPE_TO_CHANGE_LIST
import com.hussain.assistantchooser.core.KEY_THEMED_ICONS
import com.hussain.assistantchooser.core.KEY_THEME_MODE
import com.hussain.assistantchooser.core.KEY_TILE_OPEN_OVERLAY
import com.hussain.assistantchooser.core.KEY_USE_SYSTEM_FONT
import com.hussain.assistantchooser.core.OverlayHeaderButtonAction
import com.hussain.assistantchooser.core.OverlaySource
import com.hussain.assistantchooser.core.PREFS_NAME
import com.hussain.assistantchooser.core.ThemeMode
import com.hussain.assistantchooser.core.ThemedIconMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(readUiStateFromPrefs())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val customAppListManager = CustomAppListManager(application)

    val appLists: StateFlow<List<CustomAppList>> = customAppListManager.appLists
    val activeListId: StateFlow<String?> = customAppListManager.activeListId

    private val savedCustomPackages: StateFlow<List<String>> = combine(
        customAppListManager.appLists,
        customAppListManager.activeListId
    ) { lists, activeId ->
        lists.find { it.id == activeId }?.packages ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allApps: StateFlow<List<AssistantApp>> = AppCache.state
        .map { it.allApps }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoadingApps: StateFlow<Boolean> = AppCache.state
        .map { !it.isReady }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val overlayApps: StateFlow<List<AssistantApp>> = combine(
        AppCache.state,
        _uiState.map { it.overlaySource },
        savedCustomPackages
    ) { state, source, saved ->
        if (state.isReady) {
            when (source) {
                OverlaySource.ASSISTANT_APPS -> state.assistantApps
                OverlaySource.CUSTOM_APPS    -> state.allApps.filter { it.packageName in saved }
            }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveAllCustomLists(lists: List<CustomAppList>, activeId: String?) {
        customAppListManager.replaceAll(lists, activeId)
    }

    fun setActiveCustomListId(id: String) {
        customAppListManager.setActiveListId(id)
    }

    private val _updateCheckResult = MutableSharedFlow<UpdateCheckResult>()
    val updateCheckResult: SharedFlow<UpdateCheckResult> = _updateCheckResult.asSharedFlow()

    sealed class UpdateCheckResult {
        data class Available(val version: String, val url: String) : UpdateCheckResult()
        object NotAvailable : UpdateCheckResult()
        object Error : UpdateCheckResult()
    }

    private fun readUiStateFromPrefs(): SettingsUiState {
        return SettingsUiState(
            themeMode = ThemeMode.fromString(prefs.getString(KEY_THEME_MODE, null)),
            useSystemFont = prefs.getBoolean(KEY_USE_SYSTEM_FONT, false),
            openApp = prefs.getBoolean(KEY_OPEN_APP, false),
            closeAfterLaunch = prefs.getBoolean(KEY_CLOSE_AFTER_LAUNCH, true),
            showPackageName = prefs.getBoolean(KEY_SHOW_PACKAGE_NAME, false),
            showAppName = prefs.getBoolean(KEY_SHOW_APP_NAME, true),
            themedIconMode = ThemedIconMode.fromString(prefs.getString(KEY_THEMED_ICONS, null)),
            tileOpenOverlay = prefs.getBoolean(KEY_TILE_OPEN_OVERLAY, true),
            overlaySource = OverlaySource.fromString(prefs.getString(KEY_OVERLAY_SOURCE, null)),
            swipeToChangeList = prefs.getBoolean(KEY_SWIPE_TO_CHANGE_LIST, true),
            showOverlaySwitch = prefs.getBoolean(KEY_SHOW_OVERLAY_SWITCH, true),
            headerButtonAction = OverlayHeaderButtonAction.fromString(prefs.getString(KEY_OVERLAY_HEADER_BUTTON, null)),
            headerButtonLongPressAction = OverlayHeaderButtonAction.fromString(
                prefs.getString(KEY_OVERLAY_HEADER_BUTTON_LONG_PRESS, null),
                default = OverlayHeaderButtonAction.SETTINGS
            )
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setUseSystemFont(use: Boolean) {
        prefs.edit().putBoolean(KEY_USE_SYSTEM_FONT, use).apply()
        _uiState.update { it.copy(useSystemFont = use) }
    }

    fun setOpenApp(open: Boolean) {
        prefs.edit().putBoolean(KEY_OPEN_APP, open).apply()
        _uiState.update { it.copy(openApp = open) }
    }

    fun setCloseAfterLaunch(close: Boolean) {
        prefs.edit().putBoolean(KEY_CLOSE_AFTER_LAUNCH, close).apply()
        _uiState.update { it.copy(closeAfterLaunch = close) }
    }

    fun setShowPackageName(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_PACKAGE_NAME, show).apply()
        _uiState.update { it.copy(showPackageName = show) }
    }

    fun setShowAppName(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_APP_NAME, show).apply()
        _uiState.update { it.copy(showAppName = show) }
    }

    fun setThemedIconMode(mode: ThemedIconMode) {
        prefs.edit().putString(KEY_THEMED_ICONS, mode.name).apply()
        _uiState.update { it.copy(themedIconMode = mode) }
    }

    fun setTileOpenOverlay(tile: Boolean) {
        prefs.edit().putBoolean(KEY_TILE_OPEN_OVERLAY, tile).apply()
        _uiState.update { it.copy(tileOpenOverlay = tile) }
    }

    fun setOverlaySource(source: OverlaySource) {
        prefs.edit().putString(KEY_OVERLAY_SOURCE, source.name).apply()
        _uiState.update { it.copy(overlaySource = source) }
    }

    fun setSwipeToChangeList(swipe: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_TO_CHANGE_LIST, swipe).apply()
        _uiState.update { it.copy(swipeToChangeList = swipe) }
    }

    fun setShowOverlaySwitch(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_OVERLAY_SWITCH, show).apply()
        _uiState.update { it.copy(showOverlaySwitch = show) }
    }

    fun setHeaderButtonAction(action: OverlayHeaderButtonAction) {
        prefs.edit().putString(KEY_OVERLAY_HEADER_BUTTON, action.name).apply()
        _uiState.update { it.copy(headerButtonAction = action) }
    }

    fun setHeaderButtonLongPressAction(action: OverlayHeaderButtonAction) {
        prefs.edit().putString(KEY_OVERLAY_HEADER_BUTTON_LONG_PRESS, action.name).apply()
        _uiState.update { it.copy(headerButtonLongPressAction = action) }
    }

    fun reloadPrefs() {
        _uiState.value = readUiStateFromPrefs()
    }

    fun checkForUpdates() {
        if (_uiState.value.isCheckingUpdate) return
        _uiState.update { it.copy(isCheckingUpdate = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val latest = checkLatestVersionFromGitHub("Ayaanh001", "Assistant-Chooser")
            if (latest == null) {
                _updateCheckResult.emit(UpdateCheckResult.Error)
            } else {
                val tag = latest.removePrefix("v")
                if (isNewerVersion(tag, BuildConfig.VERSION_NAME)) {
                    _updateCheckResult.emit(
                        UpdateCheckResult.Available(
                            version = tag,
                            url = "https://github.com/Ayaanh001/Assistant-Chooser/releases/latest"
                        )
                    )
                } else {
                    _updateCheckResult.emit(UpdateCheckResult.NotAvailable)
                }
            }
            _uiState.update { it.copy(isCheckingUpdate = false) }
        }
    }
}
