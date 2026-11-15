package com.hussain.assistantchooser.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.lazy.itemsIndexed
import java.util.Collections
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.core.CustomAppList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAppPickerBottomSheet(
    allApps: List<AssistantApp>,
    initialAppLists: List<CustomAppList>,
    initialActiveListId: String?,
    onDismiss: () -> Unit,
    onConfirm: (List<CustomAppList>, String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var localAppLists by remember { mutableStateOf(initialAppLists) }
    var localActiveListId by remember { mutableStateOf(initialActiveListId) }
    var searchQuery       by remember { mutableStateOf("") }
    val focusManager      = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState         = rememberLazyListState()

    var showNewListDialog by remember { mutableStateOf(false) }
    var showEditListDialog by remember { mutableStateOf<CustomAppList?>(null) }
    var listNameInput by remember { mutableStateOf("") }
    
    val haptic = LocalHapticFeedback.current
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val chipWidths = remember { mutableStateMapOf<String, Float>() }
    val currentAppLists by rememberUpdatedState(localAppLists)

    if (showNewListDialog) {
        AlertDialog(
            onDismissRequest = { showNewListDialog = false },
            title = { Text("New List") },
            text = {
                OutlinedTextField(
                    value = listNameInput,
                    onValueChange = { listNameInput = it },
                    label = { Text("List Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (listNameInput.isNotBlank()) {
                            val newList = CustomAppList(name = listNameInput, packages = emptyList())
                            localAppLists = localAppLists + newList
                            localActiveListId = newList.id
                        }
                        showNewListDialog = false
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewListDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditListDialog != null) {
        val listToEdit = showEditListDialog!!
        AlertDialog(
            onDismissRequest = { showEditListDialog = null },
            title = { Text("Edit List") },
            text = {
                Column {
                    OutlinedTextField(
                        value = listNameInput,
                        onValueChange = { listNameInput = it },
                        label = { Text("List Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (listNameInput.isNotBlank()) {
                            localAppLists = localAppLists.map { 
                                if (it.id == listToEdit.id) it.copy(name = listNameInput) else it 
                            }
                        }
                        showEditListDialog = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (localAppLists.size > 1) {
                        TextButton(
                            onClick = {
                                localAppLists = localAppLists.filter { it.id != listToEdit.id }
                                if (localActiveListId == listToEdit.id) {
                                    localActiveListId = localAppLists.firstOrNull()?.id
                                }
                                showEditListDialog = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Delete") }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showEditListDialog = null }) { Text("Cancel") }
                }
            }
        )
    }

    // Dismiss keyboard when scrolling
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val initialSorted = remember(allApps, localActiveListId) {
        val currentList = localAppLists.find { it.id == localActiveListId }
        val map = currentList?.packages?.associateWith { true } ?: emptyMap()
        allApps.sortedWith(
            compareByDescending<AssistantApp> { map[it.packageName] == true }
                .thenBy { it.name.lowercase() }
        )
    }

    // Pre-convert ALL bitmaps once so LazyColumn items never do it during scroll
    val imageBitmapCache = remember(initialSorted) {
        initialSorted.associate { app -> app.packageName to app.iconBitmap.asImageBitmap() }
    }

    val filteredApps = remember(searchQuery, initialSorted) {
        if (searchQuery.isBlank()) initialSorted
        else initialSorted.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    // Reset scroll when search query changes to avoid jumping or items being hidden
    LaunchedEffect(searchQuery) {
        if (filteredApps.isNotEmpty() && (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(localActiveListId) {
        listState.scrollToItem(0)
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
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(4.dp)
                                                .size(14.dp)
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
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(localAppLists, key = { _, list -> list.id }) { index, list ->
                                val currentIndex by rememberUpdatedState(index)
                                val isDragged = draggedIndex == index
                                
                                val chipModifier = if (isDragged) {
                                    Modifier
                                        .zIndex(1f)
                                        .graphicsLayer { translationX = dragOffset }
                                } else {
                                    Modifier
                                        .animateItem()
                                        .zIndex(0f)
                                }

                                FilterChip(
                                    modifier = chipModifier
                                        .onGloballyPositioned { chipWidths[list.id] = it.size.width.toFloat() }
                                        .pointerInput(Unit) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggedIndex = currentIndex
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount.x
                                                    
                                                    val listState = currentAppLists
                                                    val curIdx = draggedIndex
                                                    if (curIdx in listState.indices) {
                                                        if (dragOffset > 0 && curIdx < listState.lastIndex) {
                                                            val nextList = listState[curIdx + 1]
                                                            val nextWidth = chipWidths[nextList.id] ?: 100.dp.toPx()
                                                            if (dragOffset > nextWidth * 0.5f) {
                                                                val newList = listState.toMutableList()
                                                                Collections.swap(newList, curIdx, curIdx + 1)
                                                                localAppLists = newList
                                                                draggedIndex = curIdx + 1
                                                                dragOffset = 0f
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            }
                                                        } else if (dragOffset < 0 && curIdx > 0) {
                                                            val prevList = listState[curIdx - 1]
                                                            val prevWidth = chipWidths[prevList.id] ?: 100.dp.toPx()
                                                            if (-dragOffset > prevWidth * 0.5f) {
                                                                val newList = listState.toMutableList()
                                                                Collections.swap(newList, curIdx, curIdx - 1)
                                                                localAppLists = newList
                                                                draggedIndex = curIdx - 1
                                                                dragOffset = 0f
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            }
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedIndex = -1
                                                    dragOffset = 0f
                                                },
                                                onDragCancel = {
                                                    draggedIndex = -1
                                                    dragOffset = 0f
                                                }
                                            )
                                        },
                                    selected = list.id == localActiveListId,
                                    onClick = { localActiveListId = list.id },
                                    label = { Text(list.name) },
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                    border = null,
                                    trailingIcon = {
                                        if (list.id == localActiveListId) {
                                            IconButton(
                                                modifier = Modifier.size(18.dp),
                                                onClick = {
                                                    listNameInput = list.name
                                                    showEditListDialog = list
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit List",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        listNameInput = ""
                                        showNewListDialog = true
                                    },
                                    label = { Text("New List") },
                                    shape = CircleShape,
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = null,
                                    leadingIcon = {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        modifier = Modifier.padding(bottom = 18.dp, end = 8.dp),
                        shape    = CircleShape,
                        onClick  = { onConfirm(localAppLists, localActiveListId) },
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
                                "No apps found for \"$searchQuery\"",
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
                    items(
                        count = filteredApps.size,
                        key   = { filteredApps[it].packageName }
                    ) { index ->
                        val app         = filteredApps[index]
                        val imageBitmap = imageBitmapCache[app.packageName]

                        // Animate corner radius to prevent "snapping" when list order changes during search
                        val isTop = index == 0
                        val isBottom = index == filteredApps.size - 1
                        val topRadius by animateDpAsState(if (isTop) 24.dp else 8.dp, label = "top")
                        val bottomRadius by animateDpAsState(if (isBottom) 24.dp else 8.dp, label = "bottom")
                        val animatedShape = RoundedCornerShape(
                            topStart = topRadius, topEnd = topRadius,
                            bottomStart = bottomRadius, bottomEnd = bottomRadius
                        )

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .clip(animatedShape)
                                .clickable {
                                    val currentList = localAppLists.find { it.id == localActiveListId }
                                    if (currentList != null) {
                                        val isCurrentlySelected = currentList.packages.contains(app.packageName)
                                        val newPackages = if (isCurrentlySelected) {
                                            currentList.packages - app.packageName
                                        } else {
                                            currentList.packages + app.packageName
                                        }
                                        localAppLists = localAppLists.map {
                                            if (it.id == localActiveListId) it.copy(packages = newPackages) else it
                                        }
                                    }

                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = animatedShape
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val currentList = localAppLists.find { it.id == localActiveListId }
                            val isChecked = currentList?.packages?.contains(app.packageName) ?: false
                            
                            Checkbox(
                                checked         = isChecked,
                                onCheckedChange = { checked ->
                                    if (currentList != null) {
                                        val newPackages = if (checked) {
                                            currentList.packages + app.packageName
                                        } else {
                                            currentList.packages - app.packageName
                                        }
                                        localAppLists = localAppLists.map {
                                            if (it.id == localActiveListId) it.copy(packages = newPackages) else it
                                        }
                                    }
                                }
                            )
                            if (imageBitmap != null) {
                                Image(
                                    bitmap             = imageBitmap,
                                    contentDescription = app.name,
                                    modifier           = Modifier.size(36.dp).clip(CircleShape)
                                )
                            } else {
                                Spacer(Modifier.size(36.dp))
                            }
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
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                    )
            )
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
