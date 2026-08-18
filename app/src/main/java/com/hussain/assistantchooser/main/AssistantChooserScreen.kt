package com.hussain.assistantchooser.main

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussain.assistantchooser.R
import com.hussain.assistantchooser.core.AppFilterMode
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.ui.components.GroupSurface
import com.hussain.assistantchooser.ui.components.SkeletonList
import com.hussain.assistantchooser.ui.components.getGroupShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantChooserScreen(
    voiceAssistants: List<AssistantApp>,
    allApps: List<AssistantApp>,
    isLoading: Boolean,
    selectedPackage: String?,
    appFilterMode: AppFilterMode,
    onAppFilterModeChange: (AppFilterMode) -> Unit,
    onAppClick: (AssistantApp) -> Unit,
    onSettingsClick: () -> Unit,
    onAddTileClicked: () -> Unit,
    onSaveCustomApps: (List<String>) -> Unit,
    savedCustomApps: Set<String>,
    showPackageName: Boolean,
    themedIcons: Boolean,
    hasShortcutHostPermission: Boolean,
) {
    val context = LocalContext.current
    var showFilterDialog    by remember { mutableStateOf(false) }
    var showCustomAppPicker by remember { mutableStateOf(false) }

    val currentAppList = remember(appFilterMode, savedCustomApps, voiceAssistants, allApps) {
        when (appFilterMode) {
            AppFilterMode.VOICE_ASSISTANTS -> voiceAssistants
            AppFilterMode.CUSTOM_APPS      -> allApps.filter { it.key in savedCustomApps }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text  = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    actions = {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.FilterAlt, contentDescription = "Filter")
                        }

                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded         = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier         = Modifier.widthIn(min = 150.dp).offset(x = 4.dp),
                            shape            = RoundedCornerShape(16.dp),
                            tonalElevation   = 3.dp,
                            shadowElevation  = 8.dp
                        ) {
                            DropdownMenuItem(
                                text        = { Text("Add Tile") },
                                onClick     = { menuExpanded = false; onAddTileClicked() },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                                modifier    = Modifier.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            )
                            HorizontalDivider(
                                modifier  = Modifier.padding(start = 48.dp, end = 16.dp),
                                thickness = 1.dp,
                                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            DropdownMenuItem(
                                text        = { Text("Settings") },
                                onClick     = { menuExpanded = false; onSettingsClick() },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                modifier    = Modifier.clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (!isLoading && appFilterMode == AppFilterMode.CUSTOM_APPS && currentAppList.isEmpty()) {
                // ── Empty state ──────────────────────────────────────────────
                Box(
                    modifier         = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(horizontal = 32.dp)
                    ) {
                        // Soft icon container
                        Surface(
                            shape          = CircleShape,
                            color          = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            tonalElevation = 0.dp,
                            modifier       = Modifier.size(88.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector        = Icons.Default.Apps,
                                    contentDescription = null,
                                    modifier           = Modifier.size(40.dp),
                                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Title
                        Text(
                            text       = "No apps selected yet",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.onSurface,
                            textAlign  = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle — explains what and why
                        Text(
                            text       = "Choose which apps appear here. You can pick any installed app — not just assistants.",
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign  = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Primary CTA
                        Button(
                            onClick = { showCustomAppPicker = true },
                            shape   = CircleShape,
                            colors  = ButtonDefaults.buttonColors()
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier           = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose apps")
                        }
                    }
                }
            } else {
                // ── App list ─────────────────────────────────────────────────
                LazyColumn(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (isLoading) {
                        item {
                            SkeletonList(count = 7, showPackageName = showPackageName)
                        }
                    }
                    itemsIndexed(
                        items = currentAppList,
                        key   = { _, app -> app.key }
                    ) { index, app ->
                        val shape = remember(index, currentAppList.size) {
                            getGroupShape(index, currentAppList.size)
                        }
                        AssistantAppRadioCard(
                            app             = app,
                            shape           = shape,
                            selected        = app.packageName == selectedPackage,
                            themedIcons     = themedIcons,
                            onSelect        = {
                                context.startActivity(
                                    Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                                Toast.makeText(
                                    context,
                                    "Select \"${app.name}\" as your default assistant",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onOpenApp       = { onAppClick(app) },
                            showPackageName = showPackageName
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                colors   = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Default Assistant Settings")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // ── Filter Bottom Sheet ───────────────────────────────────────────────────
    if (showFilterDialog) {
        ModalBottomSheet(
            onDismissRequest = { showFilterDialog = false },
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
                    text       = "App Filter",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))

                GroupSurface(count = 2) { index, shape ->
                    when (index) {
                        0 -> FilterOption(
                            text     = "Assistant Apps",
                            selected = appFilterMode == AppFilterMode.VOICE_ASSISTANTS,
                            shape    = shape,
                            onClick  = {
                                onAppFilterModeChange(AppFilterMode.VOICE_ASSISTANTS)
                                showFilterDialog = false
                            }
                        )
                        1 -> FilterOption(
                            text     = "Custom Apps",
                            selected = appFilterMode == AppFilterMode.CUSTOM_APPS,
                            shape    = shape,
                            onClick  = {
                                showFilterDialog    = false
                                showCustomAppPicker = true
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Custom App Picker Bottom Sheet ────────────────────────────────────────
    if (showCustomAppPicker) {
        CustomAppPickerBottomSheet(
            allApps          = allApps,
            selectedPackages = savedCustomApps.toList(),
            hasShortcutHostPermission = hasShortcutHostPermission,
            onDismiss        = { showCustomAppPicker = false },
            onConfirm        = { selected ->
                onSaveCustomApps(selected)
                onAppFilterModeChange(AppFilterMode.CUSTOM_APPS)
                showCustomAppPicker = false
            }
        )
    }
}
