package com.hussain.assistantchooser.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * A shimmer brush that sweeps left-to-right continuously.
 * Matches Material You surface tones so it blends with any dynamic colour scheme.
 */
@Composable
private fun shimmerBrush(): Brush {
    val base      = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue   = -600f,
        targetValue    = 1200f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    return Brush.linearGradient(
        colors      = listOf(base, highlight, base),
        start       = Offset(translateX, 0f),
        end         = Offset(translateX + 600f, 0f)
    )
}

/**
 * A single skeleton row that mirrors AssistantAppRadioCard:
 *   [circle icon]  [name block]        [radio circle]
 *                  [package block]
 */
@Composable
private fun SkeletonCard(
    shape: RoundedCornerShape,
    shimmer: Brush,
    showPackageName: Boolean
) {
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
            // App icon placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(shimmer, CircleShape)
            )

            Spacer(Modifier.width(16.dp))

            // Name + package placeholders
            Column(Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(14.dp)
                        .background(shimmer, RoundedCornerShape(7.dp))
                )
                if (showPackageName) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .height(10.dp)
                            .background(shimmer, RoundedCornerShape(5.dp))
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Radio button placeholder
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(shimmer, CircleShape)
            )
        }
    }
}

/**
 * Drop-in replacement for the real list while [AppCache] is loading.
 * Shows [count] skeleton cards with the same grouped-corner style as
 * AssistantChooserScreen.
 */
@Composable
fun SkeletonList(
    count: Int = 6,
    showPackageName: Boolean = true
) {
    val shimmer = shimmerBrush()

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(count) { i ->
            val shape = when {
                count == 1 -> RoundedCornerShape(24.dp)
                i == 0     -> RoundedCornerShape(
                    topStart = 24.dp, topEnd = 24.dp,
                    bottomStart = 8.dp, bottomEnd = 8.dp
                )
                i == count - 1 -> RoundedCornerShape(
                    topStart = 8.dp, topEnd = 8.dp,
                    bottomStart = 24.dp, bottomEnd = 24.dp
                )
                else -> RoundedCornerShape(8.dp)
            }
            SkeletonCard(
                shape           = shape,
                shimmer         = shimmer,
                showPackageName = showPackageName
            )
        }
    }
}

/**
 * A single grid cell skeleton that mirrors AppIconItem:
 *   [circle icon]
 *   [name block]   ← only when showAppName is true
 */
@Composable
private fun SkeletonGridCell(shimmer: Brush, showAppName: Boolean) {
    val iconSize = if (showAppName) 48.dp else 56.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(
            vertical   = if (showAppName) 4.dp else 10.dp,
            horizontal = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(shimmer, CircleShape)
        )
        if (showAppName) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(10.dp)
                    .background(shimmer, RoundedCornerShape(5.dp))
            )
            Spacer(Modifier.height(4.dp))
            // Second line to mimic two-line app names
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(10.dp)
                    .background(shimmer, RoundedCornerShape(5.dp))
            )
        }
    }
}

/**
 * Drop-in replacement for the overlay grid while [AppCache] is loading.
 * Shows [count] cells in a 4-column grid, matching [AssistantOverlayScreen].
 */
@Composable
fun SkeletonOverlayGrid(
    count: Int = 8,
    showAppName: Boolean = true
) {
    val shimmer = shimmerBrush()

    LazyVerticalGrid(
        columns               = GridCells.Fixed(4),
        modifier              = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
        contentPadding        = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement   = Arrangement.spacedBy(12.dp),
        userScrollEnabled     = false
    ) {
        items(count) {
            SkeletonGridCell(shimmer = shimmer, showAppName = showAppName)
        }
    }
}
