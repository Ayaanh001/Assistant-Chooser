package com.hussain.assistantchooser.settings

import com.hussain.assistantchooser.core.OverlayHeaderButtonAction
import com.hussain.assistantchooser.core.OverlaySource
import com.hussain.assistantchooser.core.ThemeMode
import com.hussain.assistantchooser.core.ThemedIconMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val useSystemFont: Boolean = false,
    val openApp: Boolean = false,
    val closeAfterLaunch: Boolean = true,
    val showPackageName: Boolean = false,
    val showAppName: Boolean = true,
    val themedIconMode: ThemedIconMode = ThemedIconMode.OFF,
    val tileOpenOverlay: Boolean = true,
    val overlaySource: OverlaySource = OverlaySource.ASSISTANT_APPS,
    val swipeToChangeList: Boolean = true,
    val showOverlaySwitch: Boolean = true,
    val headerButtonAction: OverlayHeaderButtonAction = OverlayHeaderButtonAction.APP,
    val headerButtonLongPressAction: OverlayHeaderButtonAction = OverlayHeaderButtonAction.SETTINGS,
    val isCheckingUpdate: Boolean = false
)
