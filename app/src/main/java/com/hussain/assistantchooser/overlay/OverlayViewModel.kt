package com.hussain.assistantchooser.overlay

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.assistantchooser.core.AppCache
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.core.CustomAppListManager
import com.hussain.assistantchooser.core.KEY_CLOSE_AFTER_LAUNCH
import com.hussain.assistantchooser.core.KEY_OPEN_APP
import com.hussain.assistantchooser.core.KEY_OVERLAY_HEADER_BUTTON
import com.hussain.assistantchooser.core.KEY_OVERLAY_HEADER_BUTTON_LONG_PRESS
import com.hussain.assistantchooser.core.KEY_OVERLAY_SOURCE
import com.hussain.assistantchooser.core.KEY_SHOW_APP_NAME
import com.hussain.assistantchooser.core.KEY_SHOW_OVERLAY_SWITCH
import com.hussain.assistantchooser.core.KEY_SWIPE_TO_CHANGE_LIST
import com.hussain.assistantchooser.core.KEY_THEMED_ICONS
import com.hussain.assistantchooser.core.KEY_THEME_MODE
import com.hussain.assistantchooser.core.KEY_USE_SYSTEM_FONT
import com.hussain.assistantchooser.core.OverlayHeaderButtonAction
import com.hussain.assistantchooser.core.OverlaySource
import com.hussain.assistantchooser.core.PREFS_NAME
import com.hussain.assistantchooser.core.ThemeMode
import com.hussain.assistantchooser.core.ThemedIconMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class OverlayViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = getApplication<Application>()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _overlaySource = MutableStateFlow(
        OverlaySource.fromString(prefs.getString(KEY_OVERLAY_SOURCE, null))
    )
    val overlaySource: StateFlow<OverlaySource> = _overlaySource.asStateFlow()

    val customAppListManager = CustomAppListManager(application)

    val savedCustomPackages: StateFlow<List<String>> = combine(
        customAppListManager.appLists,
        customAppListManager.activeListId
    ) { lists, activeId ->
        lists.find { it.id == activeId }?.packages ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _themeMode = MutableStateFlow(
        ThemeMode.fromString(prefs.getString(KEY_THEME_MODE, null))
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _useSystemFont = MutableStateFlow(prefs.getBoolean(KEY_USE_SYSTEM_FONT, false))
    val useSystemFont: StateFlow<Boolean> = _useSystemFont.asStateFlow()

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

    private val _swipeToChangeList = MutableStateFlow(prefs.getBoolean(KEY_SWIPE_TO_CHANGE_LIST, true))
    val swipeToChangeList: StateFlow<Boolean> = _swipeToChangeList.asStateFlow()

    private val _showOverlaySwitch = MutableStateFlow(prefs.getBoolean(KEY_SHOW_OVERLAY_SWITCH, true))
    val showOverlaySwitch: StateFlow<Boolean> = _showOverlaySwitch.asStateFlow()

    private val _headerButtonAction = MutableStateFlow(
        OverlayHeaderButtonAction.fromString(prefs.getString(KEY_OVERLAY_HEADER_BUTTON, null))
    )
    val headerButtonAction: StateFlow<OverlayHeaderButtonAction> = _headerButtonAction.asStateFlow()

    private val _headerButtonLongPressAction = MutableStateFlow(
        OverlayHeaderButtonAction.fromString(
            prefs.getString(KEY_OVERLAY_HEADER_BUTTON_LONG_PRESS, null),
            default = OverlayHeaderButtonAction.SETTINGS
        )
    )
    val headerButtonLongPressAction: StateFlow<OverlayHeaderButtonAction> = _headerButtonLongPressAction.asStateFlow()

    // Fully reactive list of apps based on source and saved selection
    val apps: StateFlow<List<AssistantApp>> = combine(
        AppCache.state,
        _overlaySource,
        savedCustomPackages
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
            KEY_OVERLAY_SOURCE -> {
                _overlaySource.value = OverlaySource.fromString(sharedPreferences.getString(KEY_OVERLAY_SOURCE, null))
            }
            KEY_THEME_MODE -> {
                _themeMode.value = ThemeMode.fromString(sharedPreferences.getString(KEY_THEME_MODE, null))
            }
            KEY_USE_SYSTEM_FONT -> {
                _useSystemFont.value = sharedPreferences.getBoolean(KEY_USE_SYSTEM_FONT, false)
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
            KEY_SWIPE_TO_CHANGE_LIST -> {
                _swipeToChangeList.value = sharedPreferences.getBoolean(KEY_SWIPE_TO_CHANGE_LIST, true)
            }
            KEY_THEMED_ICONS -> {
                _themedIconMode.value = ThemedIconMode.fromString(sharedPreferences.getString(KEY_THEMED_ICONS, null))
            }
            KEY_SHOW_OVERLAY_SWITCH -> {
                _showOverlaySwitch.value = sharedPreferences.getBoolean(KEY_SHOW_OVERLAY_SWITCH, true)
            }
            KEY_OVERLAY_HEADER_BUTTON -> {
                _headerButtonAction.value = OverlayHeaderButtonAction.fromString(sharedPreferences.getString(KEY_OVERLAY_HEADER_BUTTON, null))
            }
            KEY_OVERLAY_HEADER_BUTTON_LONG_PRESS -> {
                _headerButtonLongPressAction.value = OverlayHeaderButtonAction.fromString(
                    sharedPreferences.getString(KEY_OVERLAY_HEADER_BUTTON_LONG_PRESS, null),
                    default = OverlayHeaderButtonAction.SETTINGS
                )
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

    fun setOverlaySource(source: OverlaySource) {
        prefs.edit().putString(KEY_OVERLAY_SOURCE, source.name).apply()
        _overlaySource.value = source
    }

    fun saveCustomApps(packages: List<String>) {
        val activeId = customAppListManager.activeListId.value ?: return
        val currentList = customAppListManager.appLists.value.find { it.id == activeId }
        if (currentList != null) {
            customAppListManager.updateList(currentList.copy(packages = packages))
        } else {
            val newList = customAppListManager.addList("My Apps")
            customAppListManager.updateList(newList.copy(packages = packages))
        }
    }
}
