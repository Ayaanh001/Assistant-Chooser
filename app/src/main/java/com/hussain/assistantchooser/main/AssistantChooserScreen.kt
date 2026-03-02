package com.hussain.assistantchooser.main

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hussain.assistantchooser.core.AppFilterMode
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.ui.components.GroupSurface
import com.hussain.assistantchooser.ui.components.SkeletonList

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
    onSaveCustomApps: (List<String>) -> Unit,
    savedCustomApps: Set<String>,
    openApp: Boolean,
    closeAfterLaunch: Boolean,
    showPackageName: Boolean,
) {
    val context = LocalContext.current
    var showFilterDialog   by remember { mutableStateOf(false) }
    var showCustomAppPicker by remember { mutableStateOf(false) }
    var customAppPackages  by remember { mutableStateOf(savedCustomApps.toList()) }

    val currentAppList = remember(appFilterMode, customAppPackages, voiceAssistants, allApps) {
        when (appFilterMode) {
            AppFilterMode.VOICE_ASSISTANTS -> voiceAssistants
            AppFilterMode.CUSTOM_APPS      -> allApps.filter { it.packageName in customAppPackages }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "Assistant Chooser",
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
                            expanded          = menuExpanded,
                            onDismissRequest  = { menuExpanded = false },
                            modifier          = Modifier.widthIn(min = 150.dp).offset(x = 4.dp),
                            shape             = RoundedCornerShape(16.dp),
                            tonalElevation    = 3.dp,
                            shadowElevation   = 8.dp
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
                    color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
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
            // Use GroupSurface inside the LazyColumn via itemsIndexed
            LazyColumn(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (isLoading) {
                    item {
                        SkeletonList(count = 6, showPackageName = showPackageName)
                    }
                }
                itemsIndexed(
                    items = currentAppList,
                    key = { _, app -> app.packageName }
                ) { index, app ->
                    val shape = remember(index, currentAppList.size) {
                        when {
                            currentAppList.size == 1 -> RoundedCornerShape(24.dp)
                            index == 0 -> RoundedCornerShape(
                                topStart = 24.dp, topEnd = 24.dp,
                                bottomStart = 8.dp, bottomEnd = 8.dp
                            )

                            index == currentAppList.size - 1 -> RoundedCornerShape(
                                topStart = 8.dp, topEnd = 8.dp,
                                bottomStart = 24.dp, bottomEnd = 24.dp
                            )

                            else -> RoundedCornerShape(8.dp)
                        }
                    }
                    AssistantAppRadioCard(
                        app = app,
                        shape = shape,
                        selected = app.packageName == selectedPackage,
                        onSelect = {
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
                        onOpenApp = { onAppClick(app.packageName) },
                        showPackageName = showPackageName
                    )
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
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Default Assistant Settings")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Filter Bottom Sheet
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

    // Custom App Picker Bottom Sheet
    if (showCustomAppPicker) {
        CustomAppPickerBottomSheet(
            allApps          = allApps,
            selectedPackages = customAppPackages,
            onDismiss        = { showCustomAppPicker = false },
            onConfirm        = { selected ->
                customAppPackages = selected
                onSaveCustomApps(selected)
                onAppFilterModeChange(AppFilterMode.CUSTOM_APPS)
                showCustomAppPicker = false
            }
        )
    }
}

@Composable
fun FilterOption(
    text: String,
    selected: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val bg      = if (selected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow
    val content = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        color = bg,
        shape = shape
    ) {
        Row(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text, color = content, style = MaterialTheme.typography.titleMedium)
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAppPickerBottomSheet(
    allApps: List<AssistantApp>,
    selectedPackages: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedApps = remember {
        mutableStateMapOf<String, Boolean>().apply {
            selectedPackages.forEach { put(it, true) }
        }
    }
    var searchQuery      by remember { mutableStateOf("") }
    val focusManager     = LocalFocusManager.current
    var isKeyboardVisible by remember { mutableStateOf(false) }

    BackHandler(enabled = isKeyboardVisible) {
        focusManager.clearFocus()
        isKeyboardVisible = false
    }

    val initialSorted = remember(allApps) {
        allApps.sortedWith(
            compareByDescending<AssistantApp> { selectedApps[it.packageName] == true }
                .thenBy { it.name.lowercase() }
        )
    }

    val filteredApps = remember(searchQuery, initialSorted) {
        if (searchQuery.isBlank()) initialSorted
        else initialSorted.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
        modifier         = Modifier.fillMaxSize(),
        dragHandle       = {
            Column(Modifier.padding(top = 40.dp)) { BottomSheetDefaults.DragHandle() }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    Column {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 26.dp, vertical = 8.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Choose Apps",
                                style      = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextField(
                            value       = searchQuery,
                            onValueChange = { searchQuery = it; isKeyboardVisible = true },
                            modifier    = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .onFocusChanged { isKeyboardVisible = it.isFocused },
                            placeholder   = { Text("Search for apps…") },
                            leadingIcon   = { Icon(Icons.Default.Search, null) },
                            trailingIcon  = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, null)
                                    }
                                }
                            },
                            singleLine    = true,
                            shape         = RoundedCornerShape(32.dp),
                            colors        = TextFieldDefaults.colors(
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus(); isKeyboardVisible = false }
                            )
                        )
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        modifier = Modifier.padding(bottom = 18.dp, end = 8.dp),
                        shape    = CircleShape,
                        onClick  = { onConfirm(selectedApps.filterValues { it }.keys.toList()) },
                        icon     = { Icon(Icons.Default.Check, "Confirm") },
                        text     = { Text("Confirm") },
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier       = Modifier.padding(innerPadding).padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(bottom = 100.dp, top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = filteredApps.size,
                        key   = { filteredApps[it].packageName }
                    ) { index ->
                        val app = filteredApps[index]
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .clickable {
                                    selectedApps[app.packageName] = !(selectedApps[app.packageName] ?: false)
                                }
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    shape = CircleShape
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked         = selectedApps[app.packageName] ?: false,
                                onCheckedChange = { selectedApps[app.packageName] = it }
                            )
                            Image(
                                bitmap             = remember(app.packageName) { app.iconBitmap.asImageBitmap() },
                                contentDescription = app.name,
                                modifier           = Modifier.size(36.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(text = app.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Fade gradient at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(25.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun AssistantAppRadioCard(
    app: AssistantApp,
    shape: RoundedCornerShape,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpenApp: () -> Unit,
    showPackageName: Boolean
) {
    val iconBitmap = remember(app.packageName) { app.iconBitmap.asImageBitmap() }
    val haptic     = LocalHapticFeedback.current

    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = shape,
        color          = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(16.dp)
        ) {
            // Left area — clip to rounded shape so ripple respects the corners
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = ripple()
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenApp()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap             = iconBitmap,
                    contentDescription = app.name,
                    modifier           = Modifier.size(48.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text  = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (showPackageName) {
                        Text(
                            text     = app.packageName,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // RadioButton handles its own clipped ripple natively
            RadioButton(
                selected = selected,
                onClick  = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect()
                },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
