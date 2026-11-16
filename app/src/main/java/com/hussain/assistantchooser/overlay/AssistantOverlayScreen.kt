package com.hussain.assistantchooser.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.hussain.assistantchooser.core.OverlaySource
import com.hussain.assistantchooser.main.CustomAppPickerBottomSheet
import com.hussain.assistantchooser.ui.components.AppIconItem
import com.hussain.assistantchooser.ui.components.SkeletonOverlayGrid

private val OWN_PACKAGE = BuildConfig.APPLICATION_ID

@OptIn(ExperimentalMaterial3Api::class)
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
    onOverlaySourceChange: (OverlaySource) -> Unit = {},
    onSaveAllLists: (List<com.hussain.assistantchooser.core.CustomAppList>, String?) -> Unit,
    onSetActiveListId: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
    showAppName: Boolean,
    themedIcons: Boolean
) {
    val initialPage = remember(appLists, activeListId) {
        val index = appLists.indexOfFirst { it.id == activeListId }
        if (index >= 0) index else 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { appLists.size.coerceAtLeast(1) })

    val title = when (overlaySource) {
        OverlaySource.ASSISTANT_APPS -> "Voice Assistants"
        OverlaySource.CUSTOM_APPS    -> {
            if (swipeToChangeList && appLists.isNotEmpty()) {
                val currentList = appLists.getOrNull(pagerState.currentPage)
                currentList?.name ?: "Custom Apps"
            } else {
                val currentList = appLists.find { it.id == activeListId }
                currentList?.name ?: "Custom Apps"
            }
        }
    }
    val isCustomMode = overlaySource == OverlaySource.CUSTOM_APPS

    val haptic       = LocalHapticFeedback.current
    var showPicker   by remember { mutableStateOf(false) }

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
        targetValue   = if (visible) 1f else 0.88f,
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
            }
        )
    }

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
        Card(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .then(if (isLandscape) Modifier.widthIn(max = 360.dp) else Modifier)
                .fillMaxWidth()
                .scale(cardScale)
                .shadow(
                    elevation    = 10.dp,
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
                        .padding(horizontal = 18.dp)
                        .padding(top = 20.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AnimatedContent(
                            targetState = title,
                            transitionSpec = {
                                fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) togetherWith
                                    fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
                            },
                            modifier = Modifier.weight(1f),
                            label = "titleAnimation"
                        ) { targetTitle ->
                            Text(
                                text       = targetTitle,
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
                                                showPicker = true
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

                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = ripple(),
                                        onClick           = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onOpenApp()
                                        }
                                    )
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.OpenInNew,
                                    contentDescription = "Open Assistant Chooser",
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
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
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
                            .padding(horizontal = 10.dp)
                            .padding(top = 14.dp, bottom = 14.dp)
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
                                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
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
                                                .heightIn(max = 360.dp),
                                            contentPadding        = PaddingValues(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalArrangement   = Arrangement.spacedBy(12.dp)
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
                                        .heightIn(max = 360.dp),
                                    contentPadding        = PaddingValues(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement   = Arrangement.spacedBy(12.dp)
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
}
