package com.hussain.assistantchooser.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hussain.assistantchooser.BuildConfig
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.core.CustomAppList
import com.hussain.assistantchooser.core.OverlayHeaderButtonAction
import com.hussain.assistantchooser.core.OverlaySource
import com.hussain.assistantchooser.core.ThemeMode
import com.hussain.assistantchooser.core.ThemedIconMode
import com.hussain.assistantchooser.ui.components.ChangelogBottomSheet
import com.hussain.assistantchooser.ui.components.CollapsingTopAppBar
import com.hussain.assistantchooser.ui.components.GroupSurface
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    overlayApps: List<AssistantApp> = emptyList(),
    allApps: List<AssistantApp> = emptyList(),
    appLists: List<CustomAppList> = emptyList(),
    activeListId: String? = null,
    isLoadingApps: Boolean = false,
    onSaveAllCustomLists: (List<CustomAppList>, String?) -> Unit = { _, _ -> },
    onSetActiveCustomListId: (String) -> Unit = {},
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleUseSystemFont: (Boolean) -> Unit,
    onToggleShowPackageName: (Boolean) -> Unit,
    onToggleOpenApp: (Boolean) -> Unit,
    onToggleCloseAfter: (Boolean) -> Unit,
    onOverlaySourceChange: (OverlaySource) -> Unit,
    onHeaderButtonActionChange: (OverlayHeaderButtonAction) -> Unit,
    onHeaderButtonLongPressActionChange: (OverlayHeaderButtonAction) -> Unit,
    onToggleShowOverlaySwitch: (Boolean) -> Unit,
    onToggleShowAppName: (Boolean) -> Unit,
    onToggleTileOpenOverlay: (Boolean) -> Unit,
    onToggleSwipeToChangeList: (Boolean) -> Unit,
    onThemedIconModeChange: (ThemedIconMode) -> Unit,
    onCheckUpdate: () -> Unit,
    onExport: (exportCustomApps: Boolean, exportSettings: Boolean) -> Unit,
    onImport: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentCategory by rememberSaveable { mutableStateOf<String?>(null) }

    var showSrcSheet by remember { mutableStateOf(false) }
    var showThirdButtonSheet by remember { mutableStateOf(false) }
    var showThirdButtonLongPressSheet by remember { mutableStateOf(false) }
    var showThemedSheet by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    var exportCustom by remember { mutableStateOf(true) }
    var exportSettings by remember { mutableStateOf(true) }

    val currentVersion = BuildConfig.VERSION_NAME
    val snackbarState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val json = InputStreamReader(stream).readText()
                    onImport(json)
                }
            }.onFailure {
                Toast.makeText(context, "Failed to import settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handle back button press when inside a sub-category
    BackHandler(enabled = currentCategory != null) {
        currentCategory = null
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Settings") },
            text = {
                Column {
                    Text("Select what to export:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = exportCustom, onCheckedChange = { exportCustom = it })
                        Text("Custom App List", modifier = Modifier.clickable { exportCustom = !exportCustom })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = exportSettings, onCheckedChange = { exportSettings = it })
                        Text("Global Settings & Toggles", modifier = Modifier.clickable { exportSettings = !exportSettings })
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onExport(exportCustom, exportSettings)
                        showExportDialog = false
                    },
                    enabled = exportCustom || exportSettings
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Overlay source bottom sheet
    if (showSrcSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSrcSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            contentWindowInsets = { WindowInsets.navigationBars }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    text = "Overlay App Source",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Choose which app list is shown when the assistant button is pressed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(24.dp))

                GroupSurface(count = 2) { index, shape ->
                    when (index) {
                        0 -> OverlaySourceOption(
                            title = "Assistant Apps",
                            description = "Shows apps that support voice assistant — E.g. Google, ChatGPT, Copilot, Perplexity.",
                            selected = uiState.overlaySource == OverlaySource.ASSISTANT_APPS,
                            shape = shape,
                            onClick = {
                                onOverlaySourceChange(OverlaySource.ASSISTANT_APPS)
                                showSrcSheet = false
                            }
                        )
                        1 -> OverlaySourceOption(
                            title = "Custom Apps",
                            description = "Shows the custom list you manually picked in the main screen filter.",
                            selected = uiState.overlaySource == OverlaySource.CUSTOM_APPS,
                            shape = shape,
                            onClick = {
                                onOverlaySourceChange(OverlaySource.CUSTOM_APPS)
                                showSrcSheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showThirdButtonSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThirdButtonSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            contentWindowInsets = { WindowInsets.navigationBars }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    text = "Header Action Button",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Choose what the top-right button in the overlay header opens when tapped.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(24.dp))

                GroupSurface(count = 3) { index, shape ->
                    val option = OverlayHeaderButtonAction.entries[index]
                    val (title, description) = when (option) {
                        OverlayHeaderButtonAction.APP ->
                            "App" to "Opens the main Assistant Chooser application."
                        OverlayHeaderButtonAction.SETTINGS ->
                            "Settings" to "Opens Assistant Chooser settings."
                        OverlayHeaderButtonAction.DIGITAL_ASSISTANT_SETTINGS ->
                            "Digital assistant settings" to "Opens Android device default digital assistant settings."
                        OverlayHeaderButtonAction.NONE ->
                            "None" to "Disable long press."
                    }
                    OverlaySourceOption(
                        title = title,
                        description = description,
                        selected = uiState.headerButtonAction == option,
                        shape = shape,
                        onClick = {
                            onHeaderButtonActionChange(option)
                            showThirdButtonSheet = false
                        }
                    )
                }
            }
        }
    }

    if (showThirdButtonLongPressSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThirdButtonLongPressSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            contentWindowInsets = { WindowInsets.navigationBars }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    text = "Header Button Long Press",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Choose what the top-right button in the overlay header opens when long-pressed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(24.dp))

                GroupSurface(count = 4) { index, shape ->
                    val option = OverlayHeaderButtonAction.entries[index]
                    val (title, description) = when (option) {
                        OverlayHeaderButtonAction.APP ->
                            "App" to "Opens the main Assistant Chooser application."
                        OverlayHeaderButtonAction.SETTINGS ->
                            "Settings" to "Opens Assistant Chooser settings."
                        OverlayHeaderButtonAction.DIGITAL_ASSISTANT_SETTINGS ->
                            "Digital assistant settings" to "Opens Android device default digital assistant settings."
                        OverlayHeaderButtonAction.NONE ->
                            "None" to "Disable long press on the button."
                    }
                    OverlaySourceOption(
                        title = title,
                        description = description,
                        selected = uiState.headerButtonLongPressAction == option,
                        shape = shape,
                        onClick = {
                            onHeaderButtonLongPressActionChange(option)
                            showThirdButtonLongPressSheet = false
                        }
                    )
                }
            }
        }
    }

    if (showChangelog) {
        ChangelogBottomSheet(onDismiss = { showChangelog = false })
    }

    if (showThemedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemedSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            contentWindowInsets = { WindowInsets.navigationBars }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    text = "Themed Icons",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(24.dp))
                GroupSurface(count = 4) { index, shape ->
                    val option = ThemedIconMode.entries[index]
                    val title = when (option) {
                        ThemedIconMode.OFF -> "Off"
                        ThemedIconMode.APP_ONLY -> "App only"
                        ThemedIconMode.OVERLAY_ONLY -> "Overlay only"
                        ThemedIconMode.BOTH -> "App & Overlay"
                    }
                    ThemedIconOption(
                        title = title,
                        selected = uiState.themedIconMode == option,
                        shape = shape,
                        onClick = {
                            onThemedIconModeChange(option)
                            showThemedSheet = false
                        }
                    )
                }
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    val activeCategory = currentCategory?.let { SettingsCategory.fromId(it) }
    val topBarTitle = activeCategory?.let { stringResource(it.titleRes) } ?: "Settings"

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsingTopAppBar(
                title = topBarTitle,
                scrollBehavior = scrollBehavior,
                collapsedTitleStart = 72.dp,
                containerColor = settingsScreenBackgroundColor(),
                scrolledContainerColor = settingsScreenBackgroundColor(),
                navigationIcon = {
                    FilledIconButton(
                        onClick = {
                            if (currentCategory == null) {
                                onBack()
                            } else {
                                currentCategory = null
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = settingsTileColor()
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarState) },
        containerColor = settingsScreenBackgroundColor()
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentCategory,
                transitionSpec = {
                    if (targetState != null) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width / 3 } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width / 3 } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "SettingsCategoryTransition"
            ) { categoryId ->
                if (categoryId == null) {
                    SettingsMainHub(
                        onNavigateCategory = { currentCategory = it.id }
                    )
                } else {
                    SettingsCategoryScreen(
                        categoryId = categoryId,
                        uiState = uiState,
                        currentVersion = currentVersion,
                        overlayApps = overlayApps,
                        allApps = allApps,
                        appLists = appLists,
                        activeListId = activeListId,
                        isLoadingApps = isLoadingApps,
                        onSaveAllCustomLists = onSaveAllCustomLists,
                        onSetActiveCustomListId = onSetActiveCustomListId,
                        onThemeModeChange = onThemeModeChange,
                        onOpenThemedSheet = { showThemedSheet = true },
                        onToggleUseSystemFont = onToggleUseSystemFont,
                        onToggleShowPackageName = onToggleShowPackageName,
                        onToggleOpenApp = onToggleOpenApp,
                        onToggleCloseAfter = onToggleCloseAfter,
                        onOverlaySourceChange = onOverlaySourceChange,
                        onOpenSourceSheet = { showSrcSheet = true },
                        onOpenHeaderButtonSheet = { showThirdButtonSheet = true },
                        onOpenHeaderButtonLongPressSheet = { showThirdButtonLongPressSheet = true },
                        onToggleShowOverlaySwitch = onToggleShowOverlaySwitch,
                        onToggleShowAppName = onToggleShowAppName,
                        onToggleTileOpenOverlay = onToggleTileOpenOverlay,
                        onToggleSwipeToChangeList = onToggleSwipeToChangeList,
                        onExportClick = { showExportDialog = true },
                        onImportClick = { importLauncher.launch(arrayOf("*/*")) },
                        onShowChangelog = { showChangelog = true },
                        onCheckUpdate = onCheckUpdate
                    )
                }
            }
        }
    }
}

// ── Main Category Hub ─────────────────────────────────────────────────────────

@Composable
private fun SettingsMainHub(
    onNavigateCategory: (SettingsCategory) -> Unit
) {
    val categories = SettingsCategory.entries
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GroupSurface(count = categories.size) { index, shape ->
                val category = categories[index]
                CategoryItem(
                    title = stringResource(category.titleRes),
                    subtitle = stringResource(category.subtitleRes),
                    icon = category.icon,
                    iconColor = category.iconColor,
                    onClick = { onNavigateCategory(category) },
                    shape = shape
                )
            }
        }
    }
}

// ── Bottom Sheet Helper Options ───────────────────────────────────────────────

@Composable
fun ThemedIconOption(
    title: String,
    selected: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier.fillMaxWidth().height(64.dp).clip(shape).clickable(onClick = onClick),
        color = bgColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = contentColor)
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun OverlaySourceOption(
    title: String,
    description: String,
    selected: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick),
        color = bgColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title, style = MaterialTheme.typography.titleMedium,
                    color = contentColor, fontWeight = FontWeight.SemiBold
                )
                Text(
                    description, style = MaterialTheme.typography.bodyMedium,
                    color = if (selected)
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
