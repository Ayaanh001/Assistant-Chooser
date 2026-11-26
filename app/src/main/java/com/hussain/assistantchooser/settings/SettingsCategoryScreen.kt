package com.hussain.assistantchooser.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussain.assistantchooser.R
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.core.CustomAppList
import com.hussain.assistantchooser.core.OverlayHeaderButtonAction
import com.hussain.assistantchooser.core.OverlaySource
import com.hussain.assistantchooser.core.ThemeMode
import com.hussain.assistantchooser.core.ThemedIconMode
import com.hussain.assistantchooser.overlay.AssistantOverlayScreen
import com.hussain.assistantchooser.ui.components.GroupSurface

@Composable
fun SettingsCategoryScreen(
    categoryId: String,
    uiState: SettingsUiState,
    currentVersion: String,
    overlayApps: List<AssistantApp> = emptyList(),
    allApps: List<AssistantApp> = emptyList(),
    appLists: List<CustomAppList> = emptyList(),
    activeListId: String? = null,
    isLoadingApps: Boolean = false,
    onSaveAllCustomLists: (List<CustomAppList>, String?) -> Unit = { _, _ -> },
    onSetActiveCustomListId: (String) -> Unit = {},
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenThemedSheet: () -> Unit,
    onToggleUseSystemFont: (Boolean) -> Unit,
    onToggleShowPackageName: (Boolean) -> Unit,
    onToggleOpenApp: (Boolean) -> Unit,
    onToggleCloseAfter: (Boolean) -> Unit,
    onOverlaySourceChange: (OverlaySource) -> Unit,
    onOpenSourceSheet: () -> Unit,
    onOpenHeaderButtonSheet: () -> Unit,
    onOpenHeaderButtonLongPressSheet: () -> Unit,
    onToggleShowOverlaySwitch: (Boolean) -> Unit,
    onToggleShowAppName: (Boolean) -> Unit,
    onToggleTileOpenOverlay: (Boolean) -> Unit,
    onToggleSwipeToChangeList: (Boolean) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onShowChangelog: () -> Unit,
    onCheckUpdate: () -> Unit
) {
    val category = SettingsCategory.fromId(categoryId) ?: return
    val context = LocalContext.current

    if (category == SettingsCategory.OVERLAY) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sticky Preview Header at Top
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "LIVE OVERLAY PREVIEW",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Interactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistantOverlayScreen(
                    apps = overlayApps,
                    isLoading = isLoadingApps,
                    overlaySource = uiState.overlaySource,
                    allApps = allApps,
                    appLists = appLists,
                    activeListId = activeListId,
                    swipeToChangeList = uiState.swipeToChangeList,
                    showOverlaySwitch = uiState.showOverlaySwitch,
                    headerButtonAction = uiState.headerButtonAction,
                    headerButtonLongPressAction = uiState.headerButtonLongPressAction,
                    onOverlaySourceChange = onOverlaySourceChange,
                    onSaveAllLists = onSaveAllCustomLists,
                    onSetActiveListId = onSetActiveCustomListId,
                    onAppClick = { _ ->
                        Toast.makeText(context, "Clicked app in preview", Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = {},
                    onOpenApp = {},
                    onHeaderButtonClick = {
                        val desc = when (uiState.headerButtonAction) {
                            OverlayHeaderButtonAction.APP -> "App"
                            OverlayHeaderButtonAction.SETTINGS -> "Settings"
                            OverlayHeaderButtonAction.DIGITAL_ASSISTANT_SETTINGS -> "Digital assistant settings"
                            OverlayHeaderButtonAction.NONE -> "None"
                        }
                        Toast.makeText(context, "Header Action: $desc", Toast.LENGTH_SHORT).show()
                    },
                    onHeaderButtonLongClick = {
                        val desc = when (uiState.headerButtonLongPressAction) {
                            OverlayHeaderButtonAction.APP -> "App"
                            OverlayHeaderButtonAction.SETTINGS -> "Settings"
                            OverlayHeaderButtonAction.DIGITAL_ASSISTANT_SETTINGS -> "Digital assistant settings"
                            OverlayHeaderButtonAction.NONE -> "None"
                        }
                        Toast.makeText(context, "Header Long Press: $desc", Toast.LENGTH_SHORT).show()
                    },
                    showAppName = uiState.showAppName,
                    themedIcons = (uiState.themedIconMode == ThemedIconMode.OVERLAY_ONLY || uiState.themedIconMode == ThemedIconMode.BOTH),
                    isPreview = true
                )
            }

            // Scrollable Options List below Sticky Header
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SettingsSubsection(title = "Overlay Settings") {
                        GroupSurface(count = 7) { index, shape ->
                            when (index) {
                                0 -> SelectorSettingItem(
                                    title = "App Source for Overlay",
                                    subtitle = "Which apps appear when assistant is triggered",
                                    selectedOptionText = if (uiState.overlaySource == OverlaySource.ASSISTANT_APPS) "Assistant Apps" else "Custom Apps",
                                    onClick = onOpenSourceSheet,
                                    icon = Icons.Default.GridView,
                                    iconColor = Color(0xFF673AB7),
                                    shape = shape
                                )
                                1 -> SwitchSettingItem(
                                    title = "Show switch source button",
                                    subtitle = "Display button in overlay to switch between Assistant & Custom Apps",
                                    checked = uiState.showOverlaySwitch,
                                    onCheckedChange = onToggleShowOverlaySwitch,
                                    icon = Icons.Default.SwapHoriz,
                                    iconColor = Color(0xFF9C27B0),
                                    shape = shape
                                )
                                2 -> SelectorSettingItem(
                                    title = "Header action button",
                                    subtitle = "Action when top-right button in overlay is tapped",
                                    selectedOptionText = when (uiState.headerButtonAction) {
                                        OverlayHeaderButtonAction.APP -> "App"
                                        OverlayHeaderButtonAction.SETTINGS -> "Settings"
                                        OverlayHeaderButtonAction.DIGITAL_ASSISTANT_SETTINGS -> "Digital assistant settings"
                                        OverlayHeaderButtonAction.NONE -> "None"
                                    },
                                    onClick = onOpenHeaderButtonSheet,
                                    icon = when (uiState.headerButtonAction) {
                                        OverlayHeaderButtonAction.APP -> Icons.AutoMirrored.Filled.OpenInNew
                                        OverlayHeaderButtonAction.SETTINGS -> Icons.Default.Settings
                                        OverlayHeaderButtonAction.DIGITAL_ASSISTANT_SETTINGS -> Icons.Default.Assistant
                                        OverlayHeaderButtonAction.NONE -> Icons.AutoMirrored.Filled.OpenInNew
                                    },
                                    iconColor = Color(0xFF3F51B5),
                                    shape = shape
                                )
                                3 -> SelectorSettingItem(
                                    title = "Header button long press",
                                    subtitle = "Action when top-right button in overlay is long-pressed",
                                    selectedOptionText = when (uiState.headerButtonLongPressAction) {
                                        OverlayHeaderButtonAction.APP -> "App"
                                        OverlayHeaderButtonAction.SETTINGS -> "Settings"
                                        OverlayHeaderButtonAction.DIGITAL_ASSISTANT_SETTINGS -> "Digital assistant settings"
                                        OverlayHeaderButtonAction.NONE -> "None"
                                    },
                                    onClick = onOpenHeaderButtonLongPressSheet,
                                    icon = when (uiState.headerButtonLongPressAction) {
                                        OverlayHeaderButtonAction.APP -> Icons.AutoMirrored.Filled.OpenInNew
                                        OverlayHeaderButtonAction.SETTINGS -> Icons.Default.Settings
                                        OverlayHeaderButtonAction.DIGITAL_ASSISTANT_SETTINGS -> Icons.Default.Assistant
                                        OverlayHeaderButtonAction.NONE -> Icons.AutoMirrored.Filled.OpenInNew
                                    },
                                    iconColor = Color(0xFF009688),
                                    shape = shape
                                )
                                4 -> SwitchSettingItem(
                                    title = "Show app names",
                                    subtitle = "Display app name below each icon in overlay",
                                    checked = uiState.showAppName,
                                    onCheckedChange = onToggleShowAppName,
                                    icon = Icons.Default.Visibility,
                                    iconColor = Color(0xFF00BCD4),
                                    shape = shape
                                )
                                5 -> SwitchSettingItem(
                                    title = "Swipe to change list",
                                    subtitle = "Swipe left or right in custom apps overlay to switch lists",
                                    checked = uiState.swipeToChangeList,
                                    onCheckedChange = onToggleSwipeToChangeList,
                                    icon = Icons.Default.Swipe,
                                    iconColor = Color(0xFFE91E63),
                                    shape = shape
                                )
                                6 -> SwitchSettingItem(
                                    title = "Tile opens Overlay",
                                    subtitle = "Quick settings tile will open overlay instead of main app",
                                    checked = uiState.tileOpenOverlay,
                                    onCheckedChange = onToggleTileOpenOverlay,
                                    icon = Icons.Default.ToggleOn,
                                    iconColor = Color(0xFFFF9800),
                                    shape = shape
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                when (category) {
                    SettingsCategory.APPEARANCE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SettingsSubsection(title = "Theme") {
                                ThemeCard(
                                    selectedTheme = uiState.themeMode,
                                    onThemeChange = onThemeModeChange
                                )
                            }
                            SettingsSubsection(title = "Typography & Display") {
                                GroupSurface(count = 2) { index, shape ->
                                    when (index) {
                                        0 -> SelectorSettingItem(
                                            title = "Themed icons",
                                            subtitle = "Apply themed icons to different parts of the app",
                                            selectedOptionText = when (uiState.themedIconMode) {
                                                ThemedIconMode.OFF -> "Off"
                                                ThemedIconMode.APP_ONLY -> "App only"
                                                ThemedIconMode.OVERLAY_ONLY -> "Overlay only"
                                                ThemedIconMode.BOTH -> "App & Overlay"
                                            },
                                            onClick = onOpenThemedSheet,
                                            icon = Icons.Default.Palette,
                                            iconColor = Color(0xFFE91E63),
                                            shape = shape
                                        )
                                        1 -> SwitchSettingItem(
                                            title = "Use system font",
                                            subtitle = "Use device's default system font instead of custom app font",
                                            checked = uiState.useSystemFont,
                                            onCheckedChange = onToggleUseSystemFont,
                                            icon = Icons.Default.TextFields,
                                            iconColor = Color(0xFF009688),
                                            shape = shape
                                        )
                                    }
                                }
                            }
                        }
                    }
                    SettingsCategory.BEHAVIOUR -> {
                        SettingsSubsection(title = "App Preferences") {
                            GroupSurface(count = 3) { index, shape ->
                                when (index) {
                                    0 -> SwitchSettingItem(
                                        title = "Show package names",
                                        subtitle = "Show the app's package name below each app",
                                        checked = uiState.showPackageName,
                                        onCheckedChange = onToggleShowPackageName,
                                        icon = Icons.AutoMirrored.Filled.Label,
                                        iconColor = Color(0xFF4285F4),
                                        shape = shape
                                    )
                                    1 -> SwitchSettingItem(
                                        title = "Open App",
                                        subtitle = "Tap an app to open it instead of its voice assistant",
                                        checked = uiState.openApp,
                                        onCheckedChange = onToggleOpenApp,
                                        icon = Icons.Default.Apps,
                                        iconColor = Color(0xFF34A853),
                                        shape = shape
                                    )
                                    2 -> SwitchSettingItem(
                                        title = "Auto close after launch",
                                        subtitle = "Close Assistant Chooser after opening another app",
                                        checked = uiState.closeAfterLaunch,
                                        onCheckedChange = onToggleCloseAfter,
                                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                                        iconColor = Color(0xFFEA4335),
                                        shape = shape
                                    )
                                }
                            }
                        }
                    }
                    SettingsCategory.OVERLAY -> {}
                    SettingsCategory.BACKUP_RESTORE -> {
                        SettingsSubsection(title = "Backup & Import") {
                            GroupSurface(count = 2) { index, shape ->
                                when (index) {
                                    0 -> SettingsItem(
                                        title = "Export Settings",
                                        subtitle = "Save your configurations to a file",
                                        icon = Icons.Default.FileUpload,
                                        iconColor = Color(0xFF2196F3),
                                        onClick = onExportClick,
                                        shape = shape
                                    )
                                    1 -> SettingsItem(
                                        title = "Import Settings",
                                        subtitle = "Restore configurations from a file",
                                        icon = Icons.Default.FileDownload,
                                        iconColor = Color(0xFF8BC34A),
                                        onClick = onImportClick,
                                        shape = shape
                                    )
                                }
                            }
                        }
                    }
                    SettingsCategory.ABOUT -> {
                        SettingsSubsection {
                            GroupSurface(count = 5) { index, shape ->
                                when (index) {
                                    0 -> SettingsItem(
                                        title = "Ayaan Hussain",
                                        subtitle = "Developer",
                                        painter = painterResource(R.drawable.ah_logo),
                                        onClick = {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ayaanh001"))
                                            )
                                        },
                                        shape = shape
                                    )
                                    1 -> SettingsItem(
                                        title = "GitHub",
                                        subtitle = "Source code repository",
                                        painter = painterResource(R.drawable.ic_github),
                                        painterContainerColor = Color.Black,
                                        onClick = {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://github.com/Ayaanh001/Assistant-Chooser")
                                                )
                                            )
                                        },
                                        shape = shape
                                    )
                                    2 -> SettingsItem(
                                        title = "Telegram",
                                        subtitle = "Community & Support",
                                        icon = ImageVector.vectorResource(R.drawable.telegram),
                                        iconColor = Color(0xFF24A1DE),
                                        onClick = {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Ahacd1"))
                                            )
                                        },
                                        shape = shape
                                    )
                                    3 -> SettingsItem(
                                        title = "Changelog",
                                        subtitle = "See what's new in this version",
                                        icon = Icons.Default.History,
                                        iconColor = Color(0xFFE91E63),
                                        onClick = onShowChangelog,
                                        shape = shape
                                    )
                                    4 -> VersionTileItem(
                                        currentVersion = currentVersion,
                                        loading = uiState.isCheckingUpdate,
                                        shape = shape,
                                        onCheckUpdate = onCheckUpdate
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeCard(
    selectedTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = settingsTileColor(),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            ) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val options = ThemeMode.entries
            val count = options.size

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = selectedTheme == option

                    val shapes = when {
                        count == 1 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }

                    ToggleButton(
                        modifier = Modifier.weight(1f).height(44.dp),
                        checked = isSelected,
                        onCheckedChange = {
                            if (it) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onThemeChange(option)
                            }
                        },
                        shapes = shapes,
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = when (option) {
                                ThemeMode.AUTO -> Icons.Default.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (option) {
                                ThemeMode.AUTO -> "Auto"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionTileItem(
    currentVersion: String,
    loading: Boolean,
    shape: RoundedCornerShape,
    onCheckUpdate: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = settingsTileColor(),
        tonalElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Version", style = MaterialTheme.typography.titleMedium)
                Text(
                    currentVersion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onCheckUpdate,
                enabled = !loading,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Check updates")
                }
            }
        }
    }
}
