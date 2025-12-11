package com.hussain.assistantchooser

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hussain.assistantchooser.com.hussain.assistantchooser.AppFilterMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantChooserScreen(
    voiceAssistants: List<AssistantApp>,
    allApps: List<AssistantApp>,
    selectedPackage: String?,
    appFilterMode: AppFilterMode,
    onAppFilterModeChange: (AppFilterMode) -> Unit,
    onAppSelected: (AssistantApp) -> Unit,
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
    var showFilterDialog by remember { mutableStateOf(false) }
    var showCustomAppPicker by remember { mutableStateOf(false) }
    var customAppPackages by remember { mutableStateOf(savedCustomApps.toList()) }

    val currentAppList = remember(appFilterMode, customAppPackages) {
        when (appFilterMode) {
            AppFilterMode.VOICE_ASSISTANTS -> voiceAssistants
            AppFilterMode.CUSTOM_APPS -> allApps.filter { it.packageName in customAppPackages }
        }
    }

    val filteredApps = currentAppList

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    actions = {
                        // Filter button
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = "Filter"
                            )
                        }

                        // More button
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More"
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier
                                .widthIn(min = 150.dp)
                                .offset(x = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 3.dp,
                            shadowElevation = 8.dp
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add Tile") },
                                onClick = {
                                    menuExpanded = false
                                    onAddTileClicked()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 48.dp, end = 16.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    menuExpanded = false
                                    onSettingsClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            )
                        }
                    },
                    title = {
                        Text(
                            "Assistant Chooser",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                itemsIndexed(
                    items = filteredApps,
                    key = { _, app -> app.packageName }
                ) { index, app ->
                    val shape = remember(index, filteredApps.size) {
                        when {
                            filteredApps.size == 1 -> RoundedCornerShape(24.dp)
                            index == 0 -> RoundedCornerShape(
                                topStart = 24.dp, topEnd = 24.dp,
                                bottomStart = 8.dp, bottomEnd = 8.dp
                            )
                            index == filteredApps.size - 1 -> RoundedCornerShape(
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
                        onSelect = { onAppSelected(app) },
                        onOpenApp = { onAppClick(app.packageName) },
                        showPackageName = showPackageName
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Open Default Assistant Settings")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Filter Bottom Sheet
    if (showFilterDialog) {
        ModalBottomSheet(
            onDismissRequest = { showFilterDialog = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    text = "App Filter",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                FilterOption(
                    text = "Voice Assistants Only",
                    selected = appFilterMode == AppFilterMode.VOICE_ASSISTANTS,
                    onClick = {
                        onAppFilterModeChange(AppFilterMode.VOICE_ASSISTANTS)
                        showFilterDialog = false
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                FilterOption(
                    text = "Custom Apps",
                    selected = appFilterMode == AppFilterMode.CUSTOM_APPS,
                    onClick = {
                        showFilterDialog = false
                        showCustomAppPicker = true
                    }
                )
            }
        }

    }

    // Custom App Picker Bottom Sheet
    if (showCustomAppPicker) {
        CustomAppPickerBottomSheet(
            allApps = allApps,
            selectedPackages = customAppPackages,
            onDismiss = { showCustomAppPicker = false },
            onConfirm = { selected ->
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
    onClick: () -> Unit
) {
    val bg = if (selected)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerLow

    val content = if (selected)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(36.dp)
    )
    {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
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
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var isKeyboardVisible by remember { mutableStateOf(false) }

    // Handle back press to close keyboard first
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
        if (searchQuery.isBlank()) {
            initialSorted
        } else {
            initialSorted.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxSize(),
        dragHandle = {
            Column(Modifier.padding(top = 40.dp)) {
                BottomSheetDefaults.DragHandle()
            }
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 26.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Choose Apps",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                isKeyboardVisible = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .onFocusChanged { focusState ->
                                    isKeyboardVisible = focusState.isFocused
                                },
                            placeholder = { Text("Search for apps…") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(32.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    isKeyboardVisible = false
                                }
                            )
                        )
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        modifier = Modifier.padding(bottom = 18.dp, end = 8.dp),
                        shape = CircleShape,
                        onClick = { onConfirm(selectedApps.filterValues { it }.keys.toList()) },
                        icon = { Icon(Icons.Default.Check, "Confirm") },
                        text = { Text("Confirm") },
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(bottom = 100.dp, top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = filteredApps.size,
                        key = { index -> filteredApps[index].packageName }
                    ) { index ->
                        val app = filteredApps[index]
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .clickable {
                                    val currentSelection = selectedApps[app.packageName] ?: false
                                    selectedApps[app.packageName] = !currentSelection
                                }
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    shape = CircleShape
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedApps[app.packageName] ?: false,
                                onCheckedChange = { isChecked ->
                                    selectedApps[app.packageName] = isChecked
                                }
                            )
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = app.iconBitmap.asImageBitmap(),
                                    contentDescription = app.name,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = app.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Blur gradient at bottom
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
fun CustomAppItem(
    app: AssistantApp,
    selected: Boolean,
    onToggle: () -> Unit,
    showPackageName: Boolean
) {
    val iconBitmap = remember(app.packageName) {
        app.iconBitmap.asImageBitmap()
    }

    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }

    val shape = if (selected) {
        RoundedCornerShape(18.dp)
    } else {
        RoundedCornerShape(8.dp)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onToggle),
        color = bgColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Image(
                bitmap = iconBitmap,
                contentDescription = app.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (showPackageName) {
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
    val iconBitmap = remember(app.packageName) {
        app.iconBitmap.asImageBitmap()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
        ) {
            // Left tappable area: icon + texts -> opens the app
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenApp() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Conditionally show package name from parameter
                    if (showPackageName) {
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right area: radio button selects the assistant
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { onSelect() },
                contentAlignment = Alignment.Center
            ) {
                RadioButton(
                    selected = selected,
                    onClick = { onSelect() }
                )
            }
        }
    }
}
