package com.hussain.assistantchooser.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hussain.assistantchooser.BuildConfig
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.core.OverlayHeaderButtonAction
import com.hussain.assistantchooser.core.OverlaySource
import com.hussain.assistantchooser.main.CustomAppPickerBottomSheet
import com.hussain.assistantchooser.ui.components.AppIconItem
import com.hussain.assistantchooser.ui.components.SkeletonOverlayGrid
import kotlinx.coroutines.flow.collectLatest

private val OWN_PACKAGE = BuildConfig.APPLICATION_ID

private data class TitleInfo(val text: String, val index: Int)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AssistantOverlayScreen(
    apps: List<AssistantApp>,
    isLoading: Boolean,
    overlaySource: OverlaySource,
    allApps: List<AssistantApp>,
    appLists: List<com.hussain.assistantchooser.core.CustomAppList>,
    activeListId: String?,
    swipeToChangeList: Boolean,
    showOverlaySwitch: Boolean = true,
    headerButtonAction: OverlayHeaderButtonAction = OverlayHeaderButtonAction.APP,
    headerButtonLongPressAction: OverlayHeaderButtonAction = OverlayHeaderButtonAction.SETTINGS,
    onOverlaySourceChange: (OverlaySource) -> Unit = {},
    onSaveAllLists: (List<com.hussain.assistantchooser.core.CustomAppList>, String?) -> Unit,
    onSetActiveListId: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
    onHeaderButtonClick: () -> Unit = onOpenApp,
    onHeaderButtonLongClick: () -> Unit = {},
    showAppName: Boolean,
    themedIcons: Boolean,
    isPreview: Boolean = false
) {
    val initialPage = remember(appLists, activeListId) {
        val index = appLists.indexOfFirst { it.id == activeListId }
        if (index >= 0) index else 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { appLists.size.coerceAtLeast(1) })

    val titleInfo = remember(overlaySource, appLists, activeListId, swipeToChangeList, pagerState.currentPage) {
        when (overlaySource) {
            OverlaySource.ASSISTANT_APPS -> TitleInfo("Voice Assistants", -1)
            OverlaySource.CUSTOM_APPS    -> {
                if (swipeToChangeList && appLists.isNotEmpty()) {
                    val currentList = appLists.getOrNull(pagerState.currentPage)
                    TitleInfo(currentList?.name ?: "Custom Apps", pagerState.currentPage)
                } else {
                    val currentList = appLists.find { it.id == activeListId }
                    TitleInfo(currentList?.name ?: "Custom Apps", 0)
                }
            }
        }
    }
    val isCustomMode = overlaySource == OverlaySource.CUSTOM_APPS

    val haptic       = LocalHapticFeedback.current
    var showPicker   by rememberSaveable { mutableStateOf(false) }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(showPicker) {
        if (!showPicker) {
            val index = appLists.indexOfFirst { it.id == activeListId }
            if (index >= 0 && index != pagerState.currentPage) {
                pagerState.scrollToPage(index)
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collectLatest { page ->
            if (isCustomMode && appLists.isNotEmpty() && page in appLists.indices) {
                val listId = appLists[page].id
                if (listId != activeListId) {
                    onSetActiveListId(listId)
                }
            }
        }
    }

    val cardScale by animateFloatAsState(
        targetValue   = if (visible || isPreview) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    if (showPicker) {
        CustomAppPickerBottomSheet(
            allApps             = allApps,
            initialAppLists     = appLists,
            initialActiveListId = activeListId,
            onDismiss           = { showPicker = false },
            onConfirm           = { newLists, newActiveId -> 
                onSaveAllLists(newLists, newActiveId)
                showPicker = false
            },
            onSaveAllLists      = onSaveAllLists
        )
    }

    if (isPreview) {
        OverlayCard(
            modifier = Modifier.fillMaxWidth(),
            apps = apps,
            isLoading = isLoading,
            overlaySource = overlaySource,
            allApps = allApps,
            appLists = appLists,
            activeListId = activeListId,
            swipeToChangeList = swipeToChangeList,
            showOverlaySwitch = showOverlaySwitch,
            headerButtonAction = headerButtonAction,
            headerButtonLongPressAction = headerButtonLongPressAction,
            titleInfo = titleInfo,
            isCustomMode = isCustomMode,
            pagerState = pagerState,
            haptic = haptic,
            onOverlaySourceChange = onOverlaySourceChange,
            onShowPicker = { showPicker = true },
            onHeaderButtonClick = onHeaderButtonClick,
            onHeaderButtonLongClick = onHeaderButtonLongClick,
            onAppClick = onAppClick,
            onOpenApp = onOpenApp,
            showAppName = showAppName,
            themedIcons = themedIcons,
            isPreview = true
        )
    } else {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.30f))
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            OverlayCard(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .then(if (isLandscape) Modifier.widthIn(max = 360.dp) else Modifier)
                    .fillMaxWidth()
                    .scale(cardScale),
                apps = apps,
                isLoading = isLoading,
                overlaySource = overlaySource,
                allApps = allApps,
                appLists = appLists,
                activeListId = activeListId,
                swipeToChangeList = swipeToChangeList,
                showOverlaySwitch = showOverlaySwitch,
                headerButtonAction = headerButtonAction,
                headerButtonLongPressAction = headerButtonLongPressAction,
                titleInfo = titleInfo,
                isCustomMode = isCustomMode,
                pagerState = pagerState,
                haptic = haptic,
                onOverlaySourceChange = onOverlaySourceChange,
                onShowPicker = { showPicker = true },
                onHeaderButtonClick = onHeaderButtonClick,
                onHeaderButtonLongClick = onHeaderButtonLongClick,
                onAppClick = onAppClick,
                onOpenApp = onOpenApp,
                showAppName = showAppName,
                themedIcons = themedIcons,
                isPreview = false
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OverlayCard(
    modifier: Modifier = Modifier,
    apps: List<AssistantApp>,
    isLoading: Boolean,
    overlaySource: OverlaySource,
    allApps: List<AssistantApp>,
    appLists: List<com.hussain.assistantchooser.core.CustomAppList>,
    activeListId: String?,
    swipeToChangeList: Boolean,
    showOverlaySwitch: Boolean,
    headerButtonAction: OverlayHeaderButtonAction,
    headerButtonLongPressAction: OverlayHeaderButtonAction,
    titleInfo: TitleInfo,
    isCustomMode: Boolean,
    pagerState: androidx.compose.foundation.pager.PagerState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onOverlaySourceChange: (OverlaySource) -> Unit,
    onShowPicker: () -> Unit,
    onHeaderButtonClick: () -> Unit,
    onHeaderButtonLongClick: () -> Unit,
    onAppClick: (String) -> Unit,
    onOpenApp: () -> Unit,
    showAppName: Boolean,
    themedIcons: Boolean,
    isPreview: Boolean
) {
    val gridMaxHeight = if (isPreview) 280.dp else 360.dp
    val headerHorizontalPadding = if (isPreview) 14.dp else 18.dp
    val contentPaddingHorizontal = if (isPreview) 8.dp else 12.dp
    val gridHorizontalPadding = if (isPreview) 6.dp else 10.dp
    val gridHorizontalSpacing = if (isPreview) 6.dp else 10.dp
    val gridVerticalSpacing = if (isPreview) 8.dp else 12.dp

    Card(
        modifier = modifier
            .shadow(
                elevation    = if (isPreview) 4.dp else 10.dp,
                shape        = RoundedCornerShape(28.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                spotColor    = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            )
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = {}
            ),
        shape     = RoundedCornerShape(28.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header (Title and Buttons)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = headerHorizontalPadding)
                    .padding(top = if (isPreview) 14.dp else 20.dp, bottom = if (isPreview) 8.dp else 12.dp)
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedContent(
                        targetState = titleInfo,
                        transitionSpec = {
                            if (targetState.index > initialState.index) {
                                (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(
                                    slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(animationSpec = tween(300))
                                )
                            } else {
                                (slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn(animationSpec = tween(300))).togetherWith(
                                    slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut(animationSpec = tween(300))
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = "titleAnimation"
                    ) { targetTitleInfo ->
                        Text(
                            text       = targetTitleInfo.text,
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        if (showOverlaySwitch) {
                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = ripple(),
                                        onClick           = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val nextSource = if (overlaySource == OverlaySource.ASSISTANT_APPS) {
                                                OverlaySource.CUSTOM_APPS
                                            } else {
                                                OverlaySource.ASSISTANT_APPS
                                            }
                                            onOverlaySourceChange(nextSource)
                                        }
                                    )
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.SwapHoriz,
                                    contentDescription = if (isCustomMode) "Switch to Voice Assistants" else "Switch to Custom Apps",
                                    modifier           = Modifier.size(18.dp),
                                    tint               = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        if (isCustomMode) {
                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = ripple(),
                                        onClick           = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onShowPicker()
                                        }
                                    )
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Add,
                                    contentDescription = "Add apps",
                                    modifier           = Modifier.size(18.dp),
                                    tint               = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        val (headerIcon, headerDesc) = when (headerButtonAction) {
                            OverlayHeaderButtonAction.APP -> Icons.Default.OpenInNew to "Open Assistant Chooser"
                            OverlayHeaderButtonAction.SETTINGS -> Icons.Default.Settings to "Open Settings"
                            OverlayHeaderButtonAction.DIGITAL_ASSISTANT_SETTINGS -> Icons.Default.Assistant to "Open Digital Assistant Settings"
                            OverlayHeaderButtonAction.NONE -> Icons.Default.OpenInNew to "Open Assistant Chooser"
                        }
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = ripple(),
                                    onClick           = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onHeaderButtonClick()
                                    },
                                    onLongClick       = if (headerButtonLongPressAction != OverlayHeaderButtonAction.NONE) {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onHeaderButtonLongClick()
                                        }
                                    } else null
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = headerIcon,
                                contentDescription = headerDesc,
                                modifier           = Modifier.size(18.dp),
                                tint               = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // Content (Apps Grid)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(start = contentPaddingHorizontal, end = contentPaddingHorizontal, bottom = contentPaddingHorizontal)
                    .shadow(
                        elevation    = 6.dp,
                        shape        = RoundedCornerShape(24.dp),
                        clip         = false,
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor    = Color.Black.copy(alpha = 0.4f)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = gridHorizontalPadding)
                        .padding(top = if (isPreview) 10.dp else 14.dp, bottom = if (isPreview) 10.dp else 14.dp)
                ) {
                    when {
                        isLoading -> {
                            SkeletonOverlayGrid(
                                count       = 8,
                                showAppName = showAppName
                            )
                        }
                        isCustomMode && swipeToChangeList && appLists.isNotEmpty() -> {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize(),
                                verticalAlignment = Alignment.Top
                            ) { page ->
                                val list = appLists.getOrNull(page)
                                val pageApps = remember(list, allApps) {
                                    if (list != null) {
                                        allApps.filter { it.packageName in list.packages }
                                    } else emptyList()
                                }

                                if (pageApps.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text      = "No custom apps selected.\nTap + to build your list.",
                                            style     = MaterialTheme.typography.bodyMedium,
                                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns               = GridCells.Fixed(4),
                                        modifier              = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = gridMaxHeight),
                                        contentPadding        = PaddingValues(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(gridHorizontalSpacing),
                                        verticalArrangement   = Arrangement.spacedBy(gridVerticalSpacing)
                                    ) {
                                        items(items = pageApps, key = { it.packageName }) { app ->
                                            AppIconItem(
                                                app         = app,
                                                showAppName = showAppName,
                                                themedIcons = themedIcons,
                                                haptic      = haptic,
                                                onClick     = {
                                                    if (app.packageName == OWN_PACKAGE) onOpenApp()
                                                    else onAppClick(app.packageName)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        apps.isEmpty() && isCustomMode -> {
                            Text(
                                text      = "No custom apps selected.\nTap + to build your list.",
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            )
                        }

                        apps.isEmpty() -> {
                            Text(
                                text      = "No apps found.\nConfigure your list in settings.",
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                                )
                        }

                        else -> {
                            LazyVerticalGrid(
                                columns               = GridCells.Fixed(4),
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = gridMaxHeight),
                                contentPadding        = PaddingValues(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(gridHorizontalSpacing),
                                verticalArrangement   = Arrangement.spacedBy(gridVerticalSpacing)
                            ) {
                                items(items = apps, key = { it.packageName }) { app ->
                                    AppIconItem(
                                        app         = app,
                                        showAppName = showAppName,
                                        themedIcons = themedIcons,
                                        haptic      = haptic,
                                        onClick     = {
                                            if (app.packageName == OWN_PACKAGE) onOpenApp()
                                            else onAppClick(app.packageName)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

