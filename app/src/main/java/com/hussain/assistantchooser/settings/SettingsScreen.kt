package com.hussain.assistantchooser.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hussain.assistantchooser.BuildConfig
import com.hussain.assistantchooser.core.OverlaySource
import com.hussain.assistantchooser.ui.components.ChangelogBottomSheet
import com.hussain.assistantchooser.ui.components.GroupSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialOpenApp: Boolean,
    initialCloseAfter: Boolean,
    initialShowPackage: Boolean,
    initialOverlaySrc: OverlaySource,
    initialShowAppName: Boolean,
    onToggleOpenApp: (Boolean) -> Unit,
    onToggleCloseAfter: (Boolean) -> Unit,
    onToggleShowPackageName: (Boolean) -> Unit,
    onOverlaySourceChange: (OverlaySource) -> Unit,
    onToggleShowAppName: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context       = LocalContext.current
    var openApp       by remember { mutableStateOf(initialOpenApp) }
    var closeAfter    by remember { mutableStateOf(initialCloseAfter) }
    var showPkg       by remember { mutableStateOf(initialShowPackage) }
    var overlaySource by remember { mutableStateOf(initialOverlaySrc) }
    var showAppName   by remember { mutableStateOf(initialShowAppName) }
    var showSrcSheet  by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }

    val currentVersion = BuildConfig.VERSION_NAME
    var loading        by remember { mutableStateOf(false) }
    val snackbarState  = remember { SnackbarHostState() }
    val scope          = rememberCoroutineScope()

    // Overlay source bottom sheet
    if (showSrcSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSrcSheet = false },
            sheetState       = rememberModalBottomSheetState(),
            containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor     = MaterialTheme.colorScheme.onSurface,
            dragHandle       = { BottomSheetDefaults.DragHandle() }
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
                            icon            = Icons.Default.Label,
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
                            icon            = Icons.Default.ExitToApp,
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
                GroupSurface(count = 2) { index, shape ->
                    when (index) {
                        0 -> OverlaySourceTile(
                            overlaySource = overlaySource,
                            shape         = shape,
                            onClick       = { showSrcSheet = true }
                        )
                        1 -> SettingTile(
                            icon            = Icons.Default.Visibility,
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
                    }
                }
            }

            // About section
            item {
                SectionLabel("About")
                GroupSurface(count = 4) { index, shape ->
                    when (index) {
                        0 -> ClickableTile(
                            title    = "Ayaan Hussain",
                            subtitle = "Developer",
                            onClick  = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ayaanh001"))
                                )
                            },
                            shape = shape
                        )
                        1 -> ClickableTile(
                            title    = "GitHub",
                            subtitle = "Source code repository",
                            onClick  = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/Ayaanh001/Assistant-Chooser"))
                                )
                            },
                            shape = shape
                        )
                        2 -> ClickableTile(
                            title    = "Changelog",
                            subtitle = "See what's new in this version",
                            onClick  = { showChangelog = true },
                            shape    = shape
                        )
                        3 -> VersionTile(
                            currentVersion = currentVersion,
                            loading        = loading,
                            shape          = shape,
                            onCheckUpdate  = {
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
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: RoundedCornerShape
) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = shape,
        color          = MaterialTheme.colorScheme.surface,
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
                color    = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis, maxLines = 2)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
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
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    shape: RoundedCornerShape
) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = shape,
        color          = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    overflow = TextOverflow.Ellipsis, maxLines = 1)
            }
        }
    }
}

@Composable
private fun OverlaySourceTile(
    overlaySource: OverlaySource,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = shape,
        color          = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp)
        ) {
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.GridView, null,
                    tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("App Source for Overlay", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Which apps appear when assistant is triggered",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text     = if (overlaySource == OverlaySource.ASSISTANT_APPS) "Assistant Apps" else "Custom Apps",
                        style    = MaterialTheme.typography.labelMedium,
                        color    = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
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
        color          = MaterialTheme.colorScheme.surface,
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
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
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
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall,
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
