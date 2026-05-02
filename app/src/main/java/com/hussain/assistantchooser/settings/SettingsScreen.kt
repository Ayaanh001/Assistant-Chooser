package com.hussain.assistantchooser.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hussain.assistantchooser.BuildConfig
import com.hussain.assistantchooser.R
import com.hussain.assistantchooser.core.OverlaySource
import com.hussain.assistantchooser.ui.components.ChangelogBottomSheet
import com.hussain.assistantchooser.ui.components.GroupSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialOpenApp: Boolean,
    initialCloseAfter: Boolean,
    initialShowPackage: Boolean,
    initialOverlaySrc: OverlaySource,
    initialShowAppName: Boolean,
    initialTileOpenOverlay: Boolean,
    onToggleOpenApp: (Boolean) -> Unit,
    onToggleCloseAfter: (Boolean) -> Unit,
    onToggleShowPackageName: (Boolean) -> Unit,
    onOverlaySourceChange: (OverlaySource) -> Unit,
    onToggleShowAppName: (Boolean) -> Unit,
    onToggleTileOpenOverlay: (Boolean) -> Unit,
    onExport: (exportCustomApps: Boolean, exportSettings: Boolean) -> Unit,
    onImport: (String) -> Unit,
    onBack: () -> Unit
) {
    val context       = LocalContext.current
    var openApp       by remember { mutableStateOf(initialOpenApp) }
    var closeAfter    by remember { mutableStateOf(initialCloseAfter) }
    var showPkg       by remember { mutableStateOf(initialShowPackage) }
    var overlaySource by remember { mutableStateOf(initialOverlaySrc) }
    var showAppName   by remember { mutableStateOf(initialShowAppName) }
    var tileOpenOverlay by remember { mutableStateOf(initialTileOpenOverlay) }

    // Sync state when props change (e.g. after import)
    LaunchedEffect(initialOpenApp) { openApp = initialOpenApp }
    LaunchedEffect(initialCloseAfter) { closeAfter = initialCloseAfter }
    LaunchedEffect(initialShowPackage) { showPkg = initialShowPackage }
    LaunchedEffect(initialOverlaySrc) { overlaySource = initialOverlaySrc }
    LaunchedEffect(initialShowAppName) { showAppName = initialShowAppName }
    LaunchedEffect(initialTileOpenOverlay) { tileOpenOverlay = initialTileOpenOverlay }

    var showSrcSheet  by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    var exportCustom   by remember { mutableStateOf(true) }
    var exportSettings by remember { mutableStateOf(true) }

    val currentVersion = BuildConfig.VERSION_NAME
    var loading        by remember { mutableStateOf(false) }
    val snackbarState  = remember { SnackbarHostState() }
    val scope          = rememberCoroutineScope()

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

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title   = { Text("Export Settings") },
            text    = {
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
            sheetState       = sheetState,
            containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor     = MaterialTheme.colorScheme.onSurface,
            dragHandle       = { BottomSheetDefaults.DragHandle() },
            contentWindowInsets = { WindowInsets.navigationBars }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    text       = "Overlay App Source",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = "Choose which app list is shown when the assistant button is pressed.",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(24.dp))

                GroupSurface(count = 2) { index, shape ->
                    when (index) {
                        0 -> OverlaySourceOption(
                            title       = "Assistant Apps",
                            description = "Shows apps that support voice assistant — E.g. Google, ChatGPT, Copilot, Perplexity.",
                            selected    = overlaySource == OverlaySource.ASSISTANT_APPS,
                            shape       = shape,
                            onClick     = {
                                performHapticFeedback(context)
                                overlaySource = OverlaySource.ASSISTANT_APPS
                                onOverlaySourceChange(OverlaySource.ASSISTANT_APPS)
                                showSrcSheet = false
                            }
                        )
                        1 -> OverlaySourceOption(
                            title       = "Custom Apps",
                            description = "Shows the custom list you manually picked in the main screen filter.",
                            selected    = overlaySource == OverlaySource.CUSTOM_APPS,
                            shape       = shape,
                            onClick     = {
                                performHapticFeedback(context)
                                overlaySource = OverlaySource.CUSTOM_APPS
                                onOverlaySourceChange(OverlaySource.CUSTOM_APPS)
                                showSrcSheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showChangelog) {
        ChangelogBottomSheet(onDismiss = { showChangelog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        modifier = Modifier.padding(start = 12.dp),
                        style    = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    FilledIconButton(
                        onClick  = onBack,
                        shape    = CircleShape,
                        modifier = Modifier.padding(start = 16.dp).size(40.dp),
                        colors   = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarState) }
    ) { pad ->
        LazyColumn(
            modifier            = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Behaviour section
            item {
                SectionLabel("Behaviour")
                GroupSurface(count = 3) { index, shape ->
                    when (index) {
                        0 -> SettingTile(
                            icon            = Icons.AutoMirrored.Filled.Label,
                            iconColor       = Color(0xFF4285F4),
                            title           = "Show package names",
                            subtitle        = "Show the app's package name below each app",
                            checked         = showPkg,
                            onCheckedChange = {
                                performHapticFeedback(context)
                                showPkg = it
                                onToggleShowPackageName(it)
                            },
                            shape = shape
                        )
                        1 -> SettingTile(
                            icon            = Icons.Default.Apps,
                            iconColor       = Color(0xFF34A853),
                            title           = "Open App",
                            subtitle        = "Tap an app to open it instead of its voice assistant",
                            checked         = openApp,
                            onCheckedChange = {
                                performHapticFeedback(context)
                                openApp = it
                                onToggleOpenApp(it)
                            },
                            shape = shape
                        )
                        2 -> SettingTile(
                            icon            = Icons.AutoMirrored.Filled.ExitToApp,
                            iconColor       = Color(0xFFEA4335),
                            title           = "Auto close after launch",
                            subtitle        = "Close Assistant Chooser after opening another app",
                            checked         = closeAfter,
                            onCheckedChange = {
                                performHapticFeedback(context)
                                closeAfter = it
                                onToggleCloseAfter(it)
                            },
                            shape = shape
                        )
                    }
                }
            }

            // Overlay section
            item {
                SectionLabel("Overlay")
                GroupSurface(count = 3) { index, shape ->
                    when (index) {
                        0 -> OverlaySourceTile(
                            iconColor     = Color(0xFF673AB7),
                            overlaySource = overlaySource,
                            shape         = shape,
                            onClick       = {
                                performHapticFeedback(context)
                                showSrcSheet = true
                            }
                        )
                        1 -> SettingTile(
                            icon            = Icons.Default.Visibility,
                            iconColor       = Color(0xFF00BCD4),
                            title           = "Show app names",
                            subtitle        = "Display app name below each icon in overlay",
                            checked         = showAppName,
                            onCheckedChange = {
                                performHapticFeedback(context)
                                showAppName = it
                                onToggleShowAppName(it)
                            },
                            shape = shape
                        )
                        2 -> SettingTile(
                            icon            = Icons.Default.ToggleOn,
                            iconColor       = Color(0xFFFF9800),
                            title           = "Tile opens Overlay",
                            subtitle        = "Quick settings tile will open overlay instead of main app",
                            checked         = tileOpenOverlay,
                            onCheckedChange = {
                                performHapticFeedback(context)
                                tileOpenOverlay = it
                                onToggleTileOpenOverlay(it)
                            },
                            shape = shape
                        )
                    }
                }
            }

            // Backup & Restore
            item {
                SectionLabel("Backup & Restore")
                GroupSurface(count = 2) { index, shape ->
                    when (index) {
                        0 -> ClickableTile(
                            icon      = Icons.Default.FileUpload,
                            iconColor = Color(0xFF2196F3),
                            title     = "Export Settings",
                            subtitle  = "Save your configurations to a file",
                            onClick   = {
                                performHapticFeedback(context)
                                showExportDialog = true
                            },
                            shape     = shape
                        )
                        1 -> ClickableTile(
                            icon      = Icons.Default.FileDownload,
                            iconColor = Color(0xFF8BC34A),
                            title     = "Import Settings",
                            subtitle  = "Restore configurations from a file",
                            onClick   = {
                                performHapticFeedback(context)
                                importLauncher.launch(arrayOf("*/*"))
                            },
                            shape     = shape
                        )
                    }
                }
            }

            // About section
            item {
                SectionLabel("About")
                GroupSurface(count = 5) { index, shape ->
                    when (index) {
                        0 -> ClickableTile(
                            painter   = painterResource(R.drawable.ah_logo),
                            title     = "Ayaan Hussain",
                            subtitle  = "Developer",
                            onClick   = {
                                performHapticFeedback(context)
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ayaanh001"))
                                )
                            },
                            shape = shape
                        )
                        1 -> ClickableTile(
                            painter   = painterResource(R.drawable.ic_github),
                            painterContainerColor = Color.Black,
                            title     = "GitHub",
                            subtitle  = "Source code repository",
                            onClick   = {
                                performHapticFeedback(context)
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/Ayaanh001/Assistant-Chooser"))
                                )
                            },
                            shape = shape
                        )
                        2 -> ClickableTile(
                            icon      = ImageVector.vectorResource(R.drawable.telegram),
                            iconColor = Color(0xFF24A1DE),
                            title     = "Telegram",
                            subtitle  = "Join the community",
                            onClick   = {
                                performHapticFeedback(context)
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Ahacd1"))
                                )
                            },
                            shape = shape
                        )
                        3 -> ClickableTile(
                            icon      = Icons.Default.History,
                            iconColor = Color(0xFFE91E63),
                            title     = "Changelog",
                            subtitle  = "See what's new in this version",
                            onClick  = {
                                performHapticFeedback(context)
                                showChangelog = true
                            },
                            shape    = shape
                        )
                        4 -> VersionTile(
                            currentVersion = currentVersion,
                            loading        = loading,
                            shape          = shape,
                            onCheckUpdate  = {
                                performHapticFeedback(context)
                                loading = true
                                scope.launch(Dispatchers.IO) {
                                    val latest = checkLatestVersionFromGitHub("Ayaanh001", "Assistant-Chooser")
                                    withContext(Dispatchers.Main) {
                                        loading = false
                                        if (latest == null) {
                                            snackbarState.showSnackbar("Failed to check updates")
                                        } else {
                                            val tag = latest.removePrefix("v")
                                            if (isNewerVersion(tag, currentVersion)) {
                                                val r = snackbarState.showSnackbar(
                                                    "Update available: $tag", "Download",
                                                    duration = SnackbarDuration.Indefinite
                                                )
                                                if (r == SnackbarResult.ActionPerformed) {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW,
                                                            Uri.parse("https://github.com/Ayaanh001/Assistant-Chooser/releases/latest"))
                                                    )
                                                }
                                            } else {
                                                snackbarState.showSnackbar("No updates available")
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
    )
}

// ── Setting tiles ─────────────────────────────────────────────────────────────

@Composable
fun SettingTile(
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: RoundedCornerShape
) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = shape,
        color          = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(16.dp)
        ) {
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(icon, null, tint = iconColor,
                    modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis, maxLines = 2)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis, maxLines = 3)
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked       = checked,
                onCheckedChange = { onCheckedChange(it) },
                thumbContent  = {
                    Icon(
                        if (checked) Icons.Filled.Check else Icons.Filled.Clear,
                        null, Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        }
    }
}

@Composable
fun ClickableTile(
    icon: ImageVector? = null,
    painter: Painter? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    painterContainerColor: Color = Color.Transparent,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    shape: RoundedCornerShape
) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = shape,
        color          = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp)
        ) {
            if (icon != null || painter != null) {
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = if (painter != null) painterContainerColor else iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    if (painter != null) {
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (icon != null) {
                        Icon(icon, null, tint = iconColor,
                            modifier = Modifier.padding(8.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis, maxLines = 1)
            }
        }
    }
}

@Composable
private fun OverlaySourceTile(
    iconColor: Color = MaterialTheme.colorScheme.primary,
    overlaySource: OverlaySource,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = shape,
        color          = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp)
        ) {
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.GridView, null,
                    tint     = iconColor,
                    modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("App Source for Overlay", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Which apps appear when assistant is triggered",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                ) {
                    Text(
                        text     = if (overlaySource == OverlaySource.ASSISTANT_APPS) "Assistant Apps" else "Custom Apps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VersionTile(
    currentVersion: String,
    loading: Boolean,
    shape: RoundedCornerShape,
    onCheckUpdate: () -> Unit
) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = shape,
        color          = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Version", style = MaterialTheme.typography.titleMedium)
                Text(
                    currentVersion,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick        = onCheckUpdate,
                enabled        = !loading,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape          = RoundedCornerShape(20.dp),
                colors         = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor   = MaterialTheme.colorScheme.onTertiaryContainer
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

@Composable
fun OverlaySourceOption(
    title: String,
    description: String,
    selected: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val bgColor      = if (selected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick),
        color    = bgColor,
        shape    = shape
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = contentColor, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodyMedium,
                    color = if (selected)
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            RadioButton(
                selected = selected,
                onClick  = onClick,
                colors   = RadioButtonDefaults.colors(
                    selectedColor   = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
