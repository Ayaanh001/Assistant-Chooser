package com.hussain.assistantchooser.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

/**
 * Large collapsing top app bar that behaves like SwitchAI's CollapsingToolbarLayout
 * (titleCollapseMode="scale"): ONE title that scales and slides continuously while
 * scrolling, instead of Material3's cross-fade between two titles.
 *
 * Reuses the scroll/fling/snap logic of [TopAppBarScrollBehavior], so create it with
 * TopAppBarDefaults.exitUntilCollapsedScrollBehavior() and attach
 * scrollBehavior.nestedScrollConnection to the Scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    /** Where the title starts once collapsed (use ~72.dp when there is a back button). */
    collapsedTitleStart: Dp = 16.dp,
    expandedTitleStart: Dp = 24.dp,
    expandedTitleBottom: Dp = 20.dp,
    expandedHeight: Dp = 152.dp,
    collapsedHeight: Dp = 64.dp,
    titleFontWeight: FontWeight = FontWeight.SemiBold,
    containerColor: Color = MaterialTheme.colorScheme.background,
    scrolledContainerColor: Color = containerColor,
) {
    val density = LocalDensity.current
    val state = scrollBehavior.state

    val expandedStyle = MaterialTheme.typography.headlineLarge
    val collapsedScale = MaterialTheme.typography.titleLarge.fontSize.value /
            expandedStyle.fontSize.value

    val maxPx = with(density) { expandedHeight.toPx() }
    val minPx = with(density) { collapsedHeight.toPx() }
    val expandedStartPx = with(density) { expandedTitleStart.toPx() }
    val collapsedStartPx = with(density) { collapsedTitleStart.toPx() }
    val expandedBottomPx = with(density) { expandedTitleBottom.toPx() }
    // Centre the shrunken title vertically inside the collapsed row.
    val collapsedBottomPx = with(density) {
        (minPx - expandedStyle.lineHeight.toDp().toPx() * collapsedScale) / 2f
    }

    // Tell the scroll behavior how far the bar is allowed to collapse.
    SideEffect {
        val limit = minPx - maxPx
        if (state.heightOffsetLimit != limit) state.heightOffsetLimit = limit
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val f = state.collapsedFraction.coerceIn(0f, 1f)
                drawRect(lerpColor(containerColor, scrolledContainerColor, f))
            }
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Height is read in the layout phase only -> no recomposition while scrolling.
                .layout { measurable, constraints ->
                    val h = (maxPx + state.heightOffset).coerceIn(minPx, maxPx).roundToInt()
                    val placeable = measurable.measure(
                        constraints.copy(minHeight = h, maxHeight = h)
                    )
                    layout(placeable.width, h) { placeable.place(0, 0) }
                }
                .clipToBounds()
        ) {
            // Pinned row: navigation icon + actions never move.
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .height(collapsedHeight),
                verticalAlignment = Alignment.CenterVertically
            ) { navigationIcon() }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .height(collapsedHeight)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )

            // Single title, scaled + translated in the draw phase (smooth, no relayout).
            Text(
                text = title,
                style = expandedStyle,
                fontWeight = titleFontWeight,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .graphicsLayer {
                        val f = state.collapsedFraction.coerceIn(0f, 1f)
                        val s = lerp(1f, collapsedScale, f)
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(0f, 1f)
                        translationX = lerp(expandedStartPx, collapsedStartPx, f)
                        translationY = -lerp(expandedBottomPx, collapsedBottomPx, f)
                    }
            )
        }
    }
}
