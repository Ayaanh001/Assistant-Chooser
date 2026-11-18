package com.hussain.assistantchooser.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.core.CustomAppList

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomAppPickerBottomSheet(
    allApps: List<AssistantApp>,
    initialAppLists: List<CustomAppList>,
    initialActiveListId: String?,
    onDismiss: () -> Unit,
    onConfirm: (List<CustomAppList>, String?) -> Unit,
    onSaveAllLists: ((List<CustomAppList>, String?) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var localAppLists by remember { mutableStateOf(initialAppLists) }
    var localActiveListId by remember { mutableStateOf(initialActiveListId) }
    var searchQuery       by remember { mutableStateOf("") }
    val focusManager      = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState         = remember(localActiveListId) { LazyListState() }

    var showNewListDialog by remember { mutableStateOf(false) }
    var showEditListDialog by remember { mutableStateOf<CustomAppList?>(null) }
    var listToDelete by remember { mutableStateOf<CustomAppList?>(null) }
    var menuListId by remember { mutableStateOf<String?>(null) }
    var isReorderMode by remember { mutableStateOf(false) }
    var listNameInput by remember { mutableStateOf(TextFieldValue("")) }
    var selectedIconName by remember { mutableStateOf("Apps") }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val chipScrollState = rememberScrollState()
    val currentAppLists by rememberUpdatedState(localAppLists)

    // Reorder state. The list is NOT mutated while dragging: the dragged chip follows the finger
    // (dragDelta = px from its own slot, in scrollable-content coordinates) and every other chip
    // derives its shift from that. All chips stay composed (plain scrollable Row), so the dragged
    // chip can never be disposed while its slot is scrolled off-screen.
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableFloatStateOf(0f) }
    var dragMoved by remember { mutableStateOf(false) }
    var settling by remember { mutableStateOf(false) }
    var lastTarget by remember { mutableIntStateOf(-1) }
    var chipViewportWidth by remember { mutableFloatStateOf(0f) }
    val chipWidths = remember { mutableStateMapOf<String, Float>() }
    val chipStartPad = with(density) { 16.dp.toPx() }
    val chipGap = with(density) { 8.dp.toPx() }
    val dragScale = 1.06f

    fun slotWidth(i: Int): Float = chipWidths[currentAppLists[i].id] ?: 0f
    fun slotLeft(i: Int): Float {
        var x = chipStartPad
        for (k in 0 until i) x += slotWidth(k) + chipGap
        return x
    }

    // Keeps the dragged chip between the first and last slot.
    fun clampDelta(key: String, value: Float): Float {
        val s = currentAppLists.indexOfFirst { it.id == key }
        if (s < 0) return value
        val last = currentAppLists.lastIndex
        val minV = slotLeft(0) - slotLeft(s)
        val maxV = slotLeft(last) + slotWidth(last) - slotWidth(s) - slotLeft(s)
        return value.coerceIn(minV, maxV)
    }

    // How far (0..100%) a non-dragged chip has slid out of the way, proportional to finger movement.
    fun neighborShift(index: Int, dragKey: String, delta: Float): Float {
        val s = currentAppLists.indexOfFirst { it.id == dragKey }
        if (s < 0 || s == index || index !in currentAppLists.indices) return 0f
        val slot = slotWidth(s) + chipGap
        val dLeft = slotLeft(s) + delta
        val dRight = dLeft + slotWidth(s)
        val l = slotLeft(index)
        val w = slotWidth(index)
        if (w <= 0f) return 0f
        return if (index > s) -slot * ((dRight - l) / w).coerceIn(0f, 1f)
        else slot * ((l + w - dLeft) / w).coerceIn(0f, 1f)
    }

    // Where the dragged chip lands if released now, and the offset that snaps it exactly into that slot.
    fun resolveDrop(key: String, delta: Float): Pair<Int, Float>? {
        val s = currentAppLists.indexOfFirst { it.id == key }
        if (s < 0) return null
        val ws = slotWidth(s)
        val ls = slotLeft(s)
        val dLeft = ls + delta
        val dRight = dLeft + ws
        var target = s
        var snap = 0f
        for (j in s + 1..currentAppLists.lastIndex) {
            val l = slotLeft(j); val w = slotWidth(j)
            if (w > 0f && (dRight - l) / w >= 0.5f) { target = j; snap = l + w - ws - ls }
        }
        if (target == s) {
            for (j in s - 1 downTo 0) {
                val l = slotLeft(j); val w = slotWidth(j)
                if (w > 0f && (l + w - dLeft) / w >= 0.5f) { target = j; snap = l - ls }
            }
        }
        return target to snap
    }

    fun notifyTarget(key: String) {
        val t = resolveDrop(key, dragDelta)?.first ?: return
        if (t != lastTarget) {
            lastTarget = t
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun finishDrag() {
        val key = draggedKey ?: return
        if (settling) return
        settling = true
        scope.launch {
            try {
                val result = resolveDrop(key, dragDelta)
                if (result != null) {
                    // glide into the slot; neighbours follow automatically because they derive from dragDelta
                    Animatable(dragDelta).animateTo(
                        result.second,
                        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
                    ) { dragDelta = value }
                }
                val from = currentAppLists.indexOfFirst { it.id == key }
                val to = result?.first ?: from
                if (from >= 0 && to != from) {
                    localAppLists = currentAppLists.toMutableList().apply { add(to, removeAt(from)) }
                }
            } finally {
                draggedKey = null
                dragDelta = 0f
                dragMoved = false
                settling = false
            }
        }
    }

    // Auto-scroll the chip row while the dragged chip is held near an edge.
    LaunchedEffect(draggedKey) {
        val key = draggedKey ?: return@LaunchedEffect
        val edge = with(density) { 56.dp.toPx() }
        val maxStep = with(density) { 14.dp.toPx() }
        while (isActive) {
            withFrameNanos { }
            if (settling || !dragMoved) continue
            val s = currentAppLists.indexOfFirst { it.id == key }
            if (s < 0) continue
            val left = slotLeft(s) + dragDelta - chipScrollState.value
            val right = left + slotWidth(s)
            val dx = when {
                right > chipViewportWidth - edge ->
                    ((right - (chipViewportWidth - edge)) / edge).coerceIn(0f, 1f) * maxStep
                left < edge ->
                    -((edge - left) / edge).coerceIn(0f, 1f) * maxStep
                else -> 0f
            }
            if (dx != 0f) {
                val consumed = chipScrollState.scrollBy(dx)
                if (consumed != 0f) {
                    // content moved under the finger, so move the chip with it
                    dragDelta = clampDelta(key, dragDelta + consumed)
                    notifyTarget(key)
                }
            }
        }
    }

    // Jiggle clock: only ticks while reordering, so it costs nothing the rest of the time.
    var jigglePhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isReorderMode) {
        if (isReorderMode) {
            while (true) {
                withFrameNanos { t -> jigglePhase = ((t / 1_000_000L) % 300L) / 300f }
            }
        }
    }

    if (showNewListDialog) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        AlertDialog(
            onDismissRequest = { showNewListDialog = false },
            title = { Text("New List") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = listNameInput,
                        onValueChange = { listNameInput = it },
                        label = { Text("List Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (listNameInput.text.isNotBlank()) {
                                    val newList = CustomAppList(name = listNameInput.text.trim(), packages = emptyList(), iconName = selectedIconName)
                                    val updatedLists = localAppLists + newList
                                    localAppLists = updatedLists
                                    localActiveListId = newList.id
                                    onSaveAllLists?.invoke(updatedLists, newList.id)
                                }
                                showNewListDialog = false
                            }
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Choose Icon", style = MaterialTheme.typography.labelMedium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        items(availableListIcons) { (iconName, vector) ->
                            val isSelected = selectedIconName == iconName
                            IconButton(
                                onClick = { selectedIconName = iconName },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            ) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = iconName,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (listNameInput.text.isNotBlank()) {
                            val newList = CustomAppList(name = listNameInput.text.trim(), packages = emptyList(), iconName = selectedIconName)
                            val updatedLists = localAppLists + newList
                            localAppLists = updatedLists
                            localActiveListId = newList.id
                            onSaveAllLists?.invoke(updatedLists, newList.id)
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
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        AlertDialog(
            onDismissRequest = { showEditListDialog = null },
            title = { Text("Edit List") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = listNameInput,
                        onValueChange = { listNameInput = it },
                        label = { Text("List Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (listNameInput.text.isNotBlank()) {
                                    val updatedLists = localAppLists.map {
                                        if (it.id == listToEdit.id) it.copy(name = listNameInput.text.trim(), iconName = selectedIconName) else it
                                    }
                                    localAppLists = updatedLists
                                    onSaveAllLists?.invoke(updatedLists, localActiveListId)
                                }
                                showEditListDialog = null
                            }
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Choose Icon", style = MaterialTheme.typography.labelMedium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        items(availableListIcons) { (iconName, vector) ->
                            val isSelected = selectedIconName == iconName
                            IconButton(
                                onClick = { selectedIconName = iconName },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            ) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = iconName,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (listNameInput.text.isNotBlank()) {
                            val updatedLists = localAppLists.map {
                                if (it.id == listToEdit.id) it.copy(name = listNameInput.text.trim(), iconName = selectedIconName) else it
                            }
                            localAppLists = updatedLists
                            onSaveAllLists?.invoke(updatedLists, localActiveListId)
                        }
                        showEditListDialog = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditListDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (listToDelete != null) {
        val target = listToDelete!!
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            title = { Text("Delete List?") },
            text = { Text("Are you sure you want to delete \"${target.name}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (localAppLists.size > 1) {
                            val updatedLists = localAppLists.filter { it.id != target.id }
                            val updatedActiveId = if (localActiveListId == target.id) updatedLists.firstOrNull()?.id else localActiveListId
                            localAppLists = updatedLists
                            localActiveListId = updatedActiveId
                            onSaveAllLists?.invoke(updatedLists, updatedActiveId)
                        }
                        listToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { listToDelete = null }) { Text("Cancel") }
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
    val imageBitmapCache = remember(allApps) {
        allApps.associate { app -> app.packageName to app.iconBitmap.asImageBitmap() }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier         = Modifier.fillMaxSize(),
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        BackHandler(enabled = isReorderMode) {
            isReorderMode = false
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                topBar = {
                    Column {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
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

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { chipViewportWidth = it.width.toFloat() }
                                .horizontalScroll(chipScrollState)
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            localAppLists.forEachIndexed { index, list ->
                                key(list.id) {
                                    val isDragged = draggedKey == list.id
                                    val isSelected = list.id == localActiveListId

                                    val chipModifier = Modifier
                                        .zIndex(if (isDragged) 2f else 0f)
                                        .graphicsLayer {
                                            val dragKey = draggedKey
                                            if (dragKey == list.id) {
                                                translationX = dragDelta
                                                scaleX = dragScale
                                                scaleY = dragScale
                                            } else {
                                                if (isReorderMode) {
                                                    // each chip gets its own phase so they don't all shake in lockstep
                                                    val offset = (list.id.hashCode() and 0xFF) / 255f
                                                    rotationZ = sin((jigglePhase + offset) * 2f * PI.toFloat()) * 1.1f
                                                }
                                                if (dragKey != null) {
                                                    translationX = neighborShift(index, dragKey, dragDelta)
                                                }
                                            }
                                        }

                                    val chipTouchModifier = if (isReorderMode) {
                                        Modifier.pointerInput(Unit) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    if (draggedKey == null && !settling) {
                                                        draggedKey = list.id
                                                        dragDelta = 0f
                                                        lastTarget = currentAppLists.indexOfFirst { it.id == list.id }
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    if (!settling && draggedKey == list.id) {
                                                        dragMoved = true
                                                        // local deltas are shrunk by the chip's own scale, so undo that
                                                        dragDelta = clampDelta(list.id, dragDelta + dragAmount.x * dragScale)
                                                        notifyTarget(list.id)
                                                    }
                                                },
                                                onDragEnd = { finishDrag() },
                                                onDragCancel = { finishDrag() }
                                            )
                                        }
                                    } else {
                                        Modifier
                                            .clip(CircleShape)
                                            .combinedClickable(
                                                onClick = { localActiveListId = list.id },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuListId = list.id
                                                }
                                            )
                                    }

                                    Box(
                                        modifier = chipModifier
                                            .onSizeChanged {
                                                val w = it.width.toFloat()
                                                if (chipWidths[list.id] != w) chipWidths[list.id] = w
                                            }
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.height(32.dp).then(chipTouchModifier)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    start = 12.dp,
                                                    end = if (isReorderMode) 8.dp else 16.dp,
                                                    top = 6.dp,
                                                    bottom = 6.dp
                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = getListIconVector(list.iconName),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = list.name,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                                if (isReorderMode) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                                            .clickable { listToDelete = list },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "Delete List",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = menuListId == list.id,
                                            onDismissRequest = { menuListId = null },
                                            shape = RoundedCornerShape(24.dp),
                                            modifier = Modifier.widthIn(min = 150.dp)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Reorder") },
                                                leadingIcon = {
                                                    Box(modifier = Modifier.padding(start = 4.dp, end = 4.dp)) {
                                                        Icon(
                                                            Icons.Default.Reorder,
                                                            contentDescription = null
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    menuListId = null
                                                    isReorderMode = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Edit") },
                                                leadingIcon = {
                                                    Box(modifier = Modifier.padding(start = 4.dp, end = 4.dp)) {
                                                        Icon(
                                                            Icons.Default.Edit,
                                                            contentDescription = null
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    menuListId = null
                                                    selectedIconName = list.iconName
                                                    listNameInput = TextFieldValue(
                                                        text = list.name,
                                                        selection = TextRange(list.name.length)
                                                    )
                                                    showEditListDialog = list
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "Delete",
                                                        color = if (localAppLists.size > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                    )
                                                },
                                                leadingIcon = {
                                                    Box(modifier = Modifier.padding(start = 4.dp, end = 4.dp)) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = null,
                                                            tint = if (localAppLists.size > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                        )
                                                    }
                                                },
                                                enabled = localAppLists.size > 1,
                                                onClick = {
                                                    menuListId = null
                                                    listToDelete = list
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            if (isReorderMode) {
                                run {
                                    Surface(
                                        onClick = {
                                            isReorderMode = false
                                            onSaveAllLists?.invoke(localAppLists, localActiveListId)
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Done Reordering",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Done",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else {
                                run {
                                    Surface(
                                        onClick = {
                                            selectedIconName = "Apps"
                                            listNameInput = TextFieldValue("")
                                            showNewListDialog = true
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "New List",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "New List",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
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
                } else {
                    key(localActiveListId) {
                        LazyColumn(
                            state               = listState,
                            modifier            = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp),
                            contentPadding      = PaddingValues(bottom = 100.dp, top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            items(
                                count = filteredApps.size,
                                key   = { "${localActiveListId}_${filteredApps[it].packageName}" }
                            ) { index ->
                            val app         = filteredApps[index]
                            val imageBitmap = imageBitmapCache[app.packageName]

                            // Corners follow the row's position instantly. Animating them made the
                            // new first row's top corners "kick out" whenever the list was switched.
                            val isTop = index == 0
                            val isBottom = index == filteredApps.size - 1
                            val itemShape = RoundedCornerShape(
                                topStart = if (isTop) 24.dp else 8.dp,
                                topEnd = if (isTop) 24.dp else 8.dp,
                                bottomStart = if (isBottom) 24.dp else 8.dp,
                                bottomEnd = if (isBottom) 24.dp else 8.dp
                            )

                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .animateItem(fadeInSpec = null, fadeOutSpec = null)
                                    .clip(itemShape)
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
                                        shape = itemShape
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
            }

                // Fade gradient at bottom
//            Box(
//                modifier = Modifier
//                    .align(Alignment.BottomCenter)
//                    .fillMaxWidth()
//                    .height(25.dp)
//                    .background(
//                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
//                            listOf(
//                                Color.Transparent,
//                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
//                                MaterialTheme.colorScheme.surfaceContainerHigh
//                            )
//                        )
//                    )
//            )
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

val availableListIcons = listOf(
    "Apps" to Icons.Default.Apps,
    "Bookmark" to Icons.Default.Bookmark,
    "Star" to Icons.Default.Star,
    "Favorite" to Icons.Default.Favorite,
    "Folder" to Icons.Default.Folder,
    "AI" to Icons.Default.AutoAwesome,
    "Bot" to Icons.Default.SmartToy,
    "Laptop" to Icons.Default.Laptop,
    "Phone" to Icons.Default.Phone,
    "Car" to Icons.Default.DirectionsCar,
    "Bike" to Icons.Default.DirectionsBike,
    "Train" to Icons.Default.Train,
    "Flight" to Icons.Default.Flight,
    "Camera" to Icons.Default.CameraAlt,
    "Music" to Icons.Default.MusicNote,
    "Fitness" to Icons.Default.FitnessCenter,
    "Cafe" to Icons.Default.LocalCafe,
    "Work" to Icons.Default.Work,
    "Home" to Icons.Default.Home,
    "Game" to Icons.Default.SportsEsports,
    "School" to Icons.Default.School,
    "Shopping" to Icons.Default.ShoppingCart,
    "Person" to Icons.Default.Person,
    "Settings" to Icons.Default.Settings,
    "Menu" to Icons.Default.Menu
)

fun getListIconVector(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name) {
        "Apps" -> Icons.Default.Apps
        "Bookmark" -> Icons.Default.Bookmark
        "Star" -> Icons.Default.Star
        "Favorite" -> Icons.Default.Favorite
        "Folder" -> Icons.Default.Folder
        "AI" -> Icons.Default.AutoAwesome
        "Bot" -> Icons.Default.SmartToy
        "Laptop" -> Icons.Default.Laptop
        "Phone" -> Icons.Default.Phone
        "Car" -> Icons.Default.DirectionsCar
        "Bike" -> Icons.Default.DirectionsBike
        "Train" -> Icons.Default.Train
        "Flight" -> Icons.Default.Flight
        "Camera" -> Icons.Default.CameraAlt
        "Music" -> Icons.Default.MusicNote
        "Fitness" -> Icons.Default.FitnessCenter
        "Cafe" -> Icons.Default.LocalCafe
        "Work" -> Icons.Default.Work
        "Home" -> Icons.Default.Home
        "Game" -> Icons.Default.SportsEsports
        "School" -> Icons.Default.School
        "Shopping" -> Icons.Default.ShoppingCart
        "Person" -> Icons.Default.Person
        "Settings" -> Icons.Default.Settings
        "Menu" -> Icons.Default.Menu
        else -> Icons.Default.Apps
    }
}