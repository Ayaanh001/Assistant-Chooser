package com.hussain.assistantchooser.overlay

import androidx.compose.animation.core.Spring
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
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.core.OverlaySource
import com.hussain.assistantchooser.main.CustomAppPickerBottomSheet
import com.hussain.assistantchooser.ui.components.AppIconItem
import com.hussain.assistantchooser.ui.components.SkeletonOverlayGrid

private const val OWN_PACKAGE = "com.hussain.assistantchooser"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantOverlayScreen(
    apps: List<AssistantApp>,
    isLoading: Boolean,
    overlaySource: OverlaySource,
    allApps: List<AssistantApp>,
    savedCustomPackages: List<String>,
    onAppClick: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
    onSaveCustomApps: (List<String>) -> Unit,
    showAppName: Boolean,
) {
    val title = when (overlaySource) {
        OverlaySource.ASSISTANT_APPS -> "Voice Assistants"
        OverlaySource.CUSTOM_APPS    -> "Custom Apps"
    }
    val isCustomMode = overlaySource == OverlaySource.CUSTOM_APPS
    val haptic       = LocalHapticFeedback.current
    var showPicker   by remember { mutableStateOf(false) }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

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
            allApps          = allApps,
            selectedPackages = savedCustomPackages,
            onDismiss        = { showPicker = false },
            onConfirm        = { selected ->
                onSaveCustomApps(selected)
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
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
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(top = 20.dp, bottom = 20.dp)
            ) {
                // Header
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = title,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        modifier   = Modifier.weight(1f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        if (isCustomMode) {
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
                                    tint               = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

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
                                tint               = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Content
                when {
                    isLoading -> {
                        SkeletonOverlayGrid(
                            count       = 8,
                            showAppName = showAppName
                        )
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
