package com.hussain.assistantchooser.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.core.toBitmap

@Composable
fun AssistantAppRadioCard(
    app: AssistantApp,
    shape: RoundedCornerShape,
    selected: Boolean,
    themedIcons: Boolean,
    onSelect: () -> Unit,
    onOpenApp: () -> Unit,
    showPackageName: Boolean
) {
    val backgroundColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
    val foregroundColor = MaterialTheme.colorScheme.onPrimaryContainer.toArgb()

    val iconBitmap = remember(app.packageName, themedIcons, backgroundColor, foregroundColor) {
        if (themedIcons) {
            (app.getThemedIconBitmap(backgroundColor, foregroundColor) ?: app.iconBitmap).asImageBitmap()
        } else {
            app.iconBitmap.asImageBitmap()
        }
    }
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple()
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenApp()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = app.name,
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                    )
                    
                    if (app.shortcutId != null && app.parentIcon != null) {
                        Surface(
                            modifier = Modifier
                                .size(18.dp)
                                .offset(x = 2.dp, y = 2.dp)
                                .border(1.2.dp, MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
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
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (app.shortcutId != null || showPackageName) {
                        val subText = buildString {
                            if (app.shortcutId != null) {
                                append("Shortcut")
                                // App name is already clear from the badge icon
                            } else {
                                append(app.packageName)
                            }
                        }
                        if (subText.isNotEmpty()) {
                            Text(
                                text = subText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            RadioButton(
                selected = selected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect()
                },
                modifier = Modifier.padding(start = 0.dp)
            )
        }
    }
}
