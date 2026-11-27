package com.hussain.assistantchooser.main

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussain.assistantchooser.R
import com.hussain.assistantchooser.core.AppFilterMode
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.ui.components.CollapsingTopAppBar
import com.hussain.assistantchooser.ui.components.GroupSurface
import com.hussain.assistantchooser.ui.components.SkeletonList
import com.hussain.assistantchooser.ui.components.getGroupShape
import com.hussain.assistantchooser.ui.modifiers.BlurDirection
import com.hussain.assistantchooser.ui.modifiers.progressiveBlur

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantChooserScreen(
    voiceAssistants: List<AssistantApp>,
    allApps: List<AssistantApp>,
    isLoading: Boolean,
    selectedPackage: String?,
    appFilterMode: AppFilterMode,
    onAppFilterModeChange: (AppFilterMode) -> Unit,
    onAppClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onAddTileClicked: () -> Unit,
    savedCustomApps: Set<String>,
    appLists: List<com.hussain.assistantchooser.core.CustomAppList>,
    activeListId: String?,
    onSaveAllLists: (List<com.hussain.assistantchooser.core.CustomAppList>, String?) -> Unit,
    showPackageName: Boolean,
    themedIcons: Boolean,
) {
    val context = LocalContext.current
    var showFilterDialog    by rememberSaveable { mutableStateOf(false) }
    var showCustomAppPicker by rememberSaveable { mutableStateOf(false) }

    val currentAppList = remember(appFilterMode, savedCustomApps, voiceAssistants, allApps) {
        when (appFilterMode) {
            AppFilterMode.VOICE_ASSISTANTS -> voiceAssistants
            AppFilterMode.CUSTOM_APPS      -> allApps.filter { it.packageName in savedCustomApps }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsingTopAppBar(
                title          = stringResource(R.string.app_name),
                scrollBehavior = scrollBehavior,
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
                        offset           = DpOffset(x = (-8).dp, y = 0.dp),
                        modifier         = Modifier.widthIn(min = 130.dp).padding(horizontal = 8.dp),
                        shape            = RoundedCornerShape(24.dp),
                        containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        GroupSurface(count = 2) { index, shape ->
                            when (index) {
                                0 -> DropdownMenuItem(
                                    text           = { Text("Add Tile") },
                                    onClick        = { menuExpanded = false; onAddTileClicked() },
                                    leadingIcon    = { Icon(Icons.Default.Add, contentDescription = "Add") },
                                    contentPadding = PaddingValues(start = 12.dp, end = 24.dp),
                                    modifier       = Modifier.background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = shape).clip(shape)
                                )
                                1 -> DropdownMenuItem(
                                    text           = { Text("Settings") },
                                    onClick        = { menuExpanded = false; onSettingsClick() },
                                    leadingIcon    = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    contentPadding = PaddingValues(start = 12.dp, end = 24.dp),
                                    modifier       = Modifier.background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = shape).clip(shape)
                                )
                            }
                        }
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        // Height of the floating bottom area (button 56 + paddings). Content scrolls behind it.
        val bottomBarHeight = 56.dp + 16.dp + 8.dp
        val density = LocalDensity.current
        val blurBandPx = with(density) { (bottomBarHeight + 56.dp).toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            if (!isLoading && appFilterMode == AppFilterMode.CUSTOM_APPS && currentAppList.isEmpty()) {
                // ── Empty state ──────────────────────────────────────────────
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = bottomBarHeight + innerPadding.calculateBottomPadding()),
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
                    modifier            = Modifier
                        .fillMaxSize()
                        .progressiveBlur(
                            maxRadius  = 20.dp,
                            height     = blurBandPx,
                            direction  = BlurDirection.BOTTOM
                        ),
                    contentPadding      = PaddingValues(
                        start  = 16.dp,
                        end    = 16.dp,
                        top    = 12.dp,
                        bottom = bottomBarHeight + innerPadding.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (isLoading) {
                        item {
                            SkeletonList(count = 7, showPackageName = showPackageName)
                        }
                    }
                    itemsIndexed(
                        items = currentAppList,
                        key   = { _, app -> app.packageName }
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
                            onOpenApp       = { onAppClick(app.packageName) },
                            showPackageName = showPackageName
                        )
                    }
                }
            }

            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                colors   = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp + innerPadding.calculateBottomPadding())
                    .fillMaxWidth()
                    .height(56.dp),
                shape    = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Default Assistant Settings")
            }
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
            allApps             = allApps,
            initialAppLists     = appLists,
            initialActiveListId = activeListId,
            onDismiss           = { showCustomAppPicker = false },
            onConfirm           = { newLists, newActiveId ->
                onSaveAllLists(newLists, newActiveId)
                onAppFilterModeChange(AppFilterMode.CUSTOM_APPS)
                showCustomAppPicker = false
            },
            onSaveAllLists      = onSaveAllLists
        )
    }
}