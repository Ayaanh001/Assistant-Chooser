package com.hussain.assistantchooser.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.border
import com.hussain.assistantchooser.core.AssistantApp
import com.hussain.assistantchooser.core.toBitmap

@Composable
fun AppIconItem(
    app: AssistantApp,
    showAppName: Boolean,
    themedIcons: Boolean,
    haptic: HapticFeedback,
    onClick: () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
    val foregroundColor = MaterialTheme.colorScheme.onPrimaryContainer.toArgb()

    val bitmap = remember(app.packageName, themedIcons, backgroundColor, foregroundColor) {
        if (themedIcons) {
            app.getThemedIconBitmap(backgroundColor, foregroundColor) ?: app.iconBitmap
        } else {
            app.iconBitmap
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "appItemScale"
    )

    val iconSize = if (showAppName) 48.dp else 56.dp

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(),
                onClick           = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .padding(
                vertical   = if (showAppName) 4.dp else 10.dp,
                horizontal = 2.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = app.name,
                modifier           = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
            )
            
            if (app.shortcutId != null && app.parentIcon != null) {
                val badgeSize = iconSize * 0.35f
                Surface(
                    modifier = Modifier
                        .size(badgeSize)
                        .offset(x = 2.dp, y = 2.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Image(
                        bitmap = remember(app.packageName) { app.parentIcon.toBitmap().asImageBitmap() },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(1.dp).clip(CircleShape)
                    )
                }
            }
        }
        if (showAppName) {
            Spacer(Modifier.height(6.dp))
            val displayName = remember(app.name, app.appName, app.shortcutId) {
                if (app.shortcutId != null && app.appName != null) {
                    "${app.name}\n${app.appName}"
                } else {
                    app.name
                }
            }
            Text(
                text     = displayName,
                style    = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 72.dp)
            )
        }
    }
}
