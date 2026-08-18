package com.hussain.assistantchooser.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.core.toBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAppPickerBottomSheet(
    allApps: List<AssistantApp>,
    selectedPackages: List<String>,
    hasShortcutHostPermission: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedApps = remember {
        mutableStateMapOf<String, Boolean>().apply {
            selectedPackages.forEach { put(it, true) }
        }
    }
    var searchQuery       by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val focusManager      = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState         = rememberLazyListState()

    // Dismiss keyboard when scrolling
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val initialSorted = remember(allApps) {
        allApps.sortedWith(
            compareByDescending<AssistantApp> { selectedApps[it.key] == true }
                .thenBy { it.name.lowercase() }
        )
    }

    val appsOnly = remember(allApps) {
        allApps.filter { it.shortcutId == null }
            .sortedWith(
                compareByDescending<AssistantApp> { selectedApps[it.key] == true }
                    .thenBy { it.name.lowercase() }
            )
    }

    val shortcutsOnly = remember(allApps) {
        allApps.filter { it.shortcutId != null }
            .sortedWith(
                compareByDescending<AssistantApp> { selectedApps[it.key] == true }
                    .thenBy { (it.appName ?: it.name).lowercase() }
                    .thenBy { it.name.lowercase() }
            )
    }

    // Pre-convert ALL bitmaps once so LazyColumn items never do it during scroll
    val imageBitmapCache = remember(initialSorted) {
        initialSorted.associate { app -> app.key to app.iconBitmap.asImageBitmap() }
    }

    val filteredApps = remember(searchQuery, selectedTabIndex, appsOnly, shortcutsOnly) {
        val baseList = if (selectedTabIndex == 0) appsOnly else shortcutsOnly
        if (searchQuery.isBlank()) baseList
        else baseList.filter { 
            it.name.contains(searchQuery, ignoreCase = true) ||
            (it.appName?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    // Reset scroll when search query or tab changes to avoid jumping or items being hidden
    LaunchedEffect(searchQuery, selectedTabIndex) {
        if (filteredApps.isNotEmpty() && (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)) {
            listState.scrollToItem(0)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier         = Modifier.fillMaxSize(),
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                        val focusManager = LocalFocusManager.current
                        val keyboardController = LocalSoftwareKeyboardController.current

                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                            placeholder = { Text("Search for apps…") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                                            .clickable {
                                                searchQuery = ""
                                                focusManager.clearFocus(force = true)
                                                keyboardController?.hide()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },

                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                }
                            ),
                            shape = RoundedCornerShape(32.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        val haptic = LocalHapticFeedback.current
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            val indicatorWidth = this.maxWidth / 2
                            val indicatorOffset by animateDpAsState(
                                targetValue = if (selectedTabIndex == 0) 0.dp else indicatorWidth,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "tabIndicator"
                            )

                            // Sliding selection pill
                            Box(
                                modifier = Modifier
                                    .offset(x = indicatorOffset)
                                    .width(indicatorWidth)
                                    .height(48.dp)
                                    .padding(4.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50.dp))
                            )

                            Row(modifier = Modifier.fillMaxWidth()) {
                                listOf(0 to "Apps", 1 to "Shortcuts").forEach { (index, label) ->
                                    val isSelected = selectedTabIndex == index
                                    val textColor by animateColorAsState(
                                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                                      else MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "tabTextColor"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                if (selectedTabIndex != index) {
                                                    selectedTabIndex = index
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }
                            }
                        }
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
                if (filteredApps.isEmpty() && searchQuery.isNotEmpty()) {
                    val suggestedApp = remember(searchQuery) {
                        if (searchQuery.length < 2) null
                        else initialSorted.find {
                            val name = it.name.lowercase()
                            val query = searchQuery.lowercase()
                            name.startsWith(query) ||
                                    calculateLevenshteinDistance(name, query) <= 2
                        }
                    }

                    Box(
                        modifier         = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier           = Modifier.size(64.dp),
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (selectedTabIndex == 0) "No apps found for \"$searchQuery\""
                                else "No shortcuts found for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (suggestedApp != null) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        append("Did you mean ")
                                        withStyle(
                                            SpanStyle(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            append(suggestedApp.name)
                                        }
                                        append("?")
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            CircleShape
                                        )
                                        .clip(CircleShape)
                                        .clickable {
                                            searchQuery = suggestedApp.name
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    state               = listState,
                    modifier            = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 14.dp),
                    contentPadding      = PaddingValues(bottom = 100.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (selectedTabIndex == 0) {
                        // Apps Tab - Flat list
                        items(
                            count = filteredApps.size,
                            key   = { filteredApps[it].key }
                        ) { index ->
                            val app = filteredApps[index]
                            AppPickerItem(
                                app = app,
                                isSelected = selectedApps[app.key] ?: false,
                                imageBitmap = imageBitmapCache[app.key],
                                isTop = index == 0,
                                isBottom = index == filteredApps.size - 1,
                                showBadge = false,
                                onToggle = {
                                    selectedApps[app.key] = !(selectedApps[app.key] ?: false)
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            )
                        }
                    } else {
                        // Shortcuts Tab - Grouped by app
                        val grouped = filteredApps.groupBy { it.packageName }
                        grouped.forEach { (pkg, shortcuts) ->
                            item(key = "header_$pkg") {
                                val firstShortcut = shortcuts.firstOrNull()
                                val appLabel = firstShortcut?.appName ?: pkg
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                ) {
                                    if (firstShortcut?.parentIcon != null) {
                                        Image(
                                            bitmap = remember(pkg) { firstShortcut.parentIcon.toBitmap().asImageBitmap() },
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp).clip(CircleShape)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    Text(
                                        text = appLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            items(
                                count = shortcuts.size,
                                key = { shortcuts[it].key }
                            ) { index ->
                                val app = shortcuts[index]
                                val isCurrentlyLaunchable = app.isLaunchable || hasShortcutHostPermission
                                AppPickerItem(
                                    app = app,
                                    isSelected = selectedApps[app.key] ?: false,
                                    imageBitmap = imageBitmapCache[app.key],
                                    isTop = index == 0,
                                    isBottom = index == shortcuts.size - 1,
                                    showBadge = false,
                                    isLaunchable = isCurrentlyLaunchable,
                                    onToggle = {
                                        selectedApps[app.key] = !(selectedApps[app.key] ?: false)
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }
                                )
                            }
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
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun AppPickerItem(
    app: AssistantApp,
    isSelected: Boolean,
    imageBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    isTop: Boolean,
    isBottom: Boolean,
    showBadge: Boolean = true,
    isLaunchable: Boolean = true,
    onToggle: () -> Unit
) {
    val topRadius by animateDpAsState(if (isTop) 24.dp else 8.dp, label = "top")
    val bottomRadius by animateDpAsState(if (isBottom) 24.dp else 8.dp, label = "bottom")
    val animatedShape = RoundedCornerShape(
        topStart = topRadius, topEnd = topRadius,
        bottomStart = bottomRadius, bottomEnd = bottomRadius
    )

    Row(
        Modifier
            .fillMaxWidth()
            .clip(animatedShape)
            .clickable { onToggle() }
            .alpha(if (isLaunchable) 1f else 0.5f)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = animatedShape
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked         = isSelected,
            onCheckedChange = { onToggle() }
        )
        if (imageBitmap != null) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Image(
                    bitmap             = imageBitmap,
                    contentDescription = app.name,
                    modifier           = Modifier.size(36.dp).clip(CircleShape)
                )
                if (showBadge && app.shortcutId != null && app.parentIcon != null) {
                    Surface(
                        modifier = Modifier
                            .size(14.dp)
                            .offset(x = 1.dp, y = 1.dp)
                            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 1.dp
                    ) {
                        Image(
                            bitmap = remember(app.packageName) { app.parentIcon.toBitmap().asImageBitmap() },
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(1.dp).clip(CircleShape)
                        )
                    }
                }
            }
        } else {
            Spacer(Modifier.size(36.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.name, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!isLaunchable) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Requires default launcher",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
    if (s1 == s2) return 0
    if (s1.isEmpty()) return s2.length
    if (s2.isEmpty()) return s1.length

    val dp = IntArray(s2.length + 1) { it }
    for (i in 1..s1.length) {
        var prev = i
        for (j in 1..s2.length) {
            val next = if (s1[i - 1] == s2[j - 1]) dp[j - 1] else minOf(dp[j - 1], dp[j], prev) + 1
            dp[j - 1] = prev
            prev = next
        }
        dp[s2.length] = prev
    }
    return dp[s2.length]
}
