package com.hussain.assistantchooser.ui.modifiers

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class BlurDirection { TOP, BOTTOM }

/**
 * Progressive blur drawn with a custom AGSL shader, so it only needs stable Compose APIs
 * (no Compose 1.13 alpha). The blur grows from 0 at the inner edge of the band to
 * [maxRadius] at the screen edge.
 *
 *  - Android 13+ (API 33): real progressive blur.
 *  - Older versions / battery saver: only the soft fade overlay is drawn.
 *
 * @param maxRadius blur radius at the very edge
 * @param height    height of the blurred band, in px
 */
fun Modifier.progressiveBlur(
    maxRadius: Dp = 20.dp,
    height: Float,
    direction: BlurDirection = BlurDirection.BOTTOM,
    showGradientOverlay: Boolean = true,
    overlayColor: Color? = null,
): Modifier = composed {
    if (height <= 0f) return@composed this

    val context = LocalContext.current
    val density = LocalDensity.current
    val scrimColor = (overlayColor ?: MaterialTheme.colorScheme.background).copy(alpha = 0.55f)
    val isPowerSave = remember(context) {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
    }
    val maxRadiusPx = with(density) { maxRadius.toPx() }

    // Outer: fade overlay (drawn on top of the blurred layer below)
    val scrim = if (showGradientOverlay) {
        Modifier.drawWithContent {
            drawContent()
            val brush = when (direction) {
                BlurDirection.TOP -> Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to scrimColor,
                        0.5f to scrimColor.copy(alpha = scrimColor.alpha * 0.35f),
                        1.0f to Color.Transparent,
                    ),
                    startY = 0f,
                    endY = height,
                )
                BlurDirection.BOTTOM -> Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.5f to scrimColor.copy(alpha = scrimColor.alpha * 0.35f),
                        1.0f to scrimColor,
                    ),
                    startY = size.height - height,
                    endY = size.height,
                )
            }
            drawRect(brush = brush)
        }
    } else Modifier

    // Inner: the blur itself
    val blur = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isPowerSave && maxRadiusPx > 0f) {
        Modifier.shaderProgressiveBlur(maxRadiusPx, height, direction)
    } else Modifier

    this.then(scrim).then(blur)
}

private const val PROGRESSIVE_BLUR_AGSL = """
    uniform shader composable;
    uniform float2 size;
    uniform float bandHeight;
    uniform float maxRadius;
    uniform float fromBottom;

    half4 main(float2 c) {
        // distance into the band: 0 at inner edge -> 1 at screen edge
        float d = (fromBottom > 0.5) ? (c.y - (size.y - bandHeight)) : (bandHeight - c.y);
        float t = clamp(d / bandHeight, 0.0, 1.0);
        // very gentle start, then ramps up
        t = t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
        float r = maxRadius * t;

        half4 center = composable.eval(c);
        if (r < 0.6) { return center; }

        // per-pixel random rotation (interleaved gradient noise) turns
        // visible ring/banding artifacts into very fine, unnoticeable noise
        float ign = fract(52.9829189 * fract(dot(c, float2(0.06711056, 0.00583715))));
        float rot = ign * 6.2831853;

        half4 acc = center;
        float w = 1.0;
        const int N = 64;
        for (int i = 1; i <= N; i++) {
            float f = float(i) / float(N);
            float a = float(i) * 2.39996323 + rot;
            float rad = sqrt(f) * r;
            float2 p = c + rad * float2(cos(a), sin(a));
            p = clamp(p, float2(0.5, 0.5), size - float2(0.5, 0.5));
            // gaussian-like falloff: centre taps count more, edges soften smoothly
            float wi = exp(-2.2 * f);
            acc += composable.eval(p) * wi;
            w += wi;
        }
        return acc / w;
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Modifier.shaderProgressiveBlur(
    maxRadiusPx: Float,
    bandHeightPx: Float,
    direction: BlurDirection,
): Modifier = composed {
    val shader = remember { RuntimeShader(PROGRESSIVE_BLUR_AGSL) }
    graphicsLayer {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("bandHeight", bandHeightPx.coerceAtMost(size.height))
        shader.setFloatUniform("maxRadius", maxRadiusPx)
        shader.setFloatUniform("fromBottom", if (direction == BlurDirection.BOTTOM) 1f else 0f)
        renderEffect = RenderEffect
            .createRuntimeShaderEffect(shader, "composable")
            .asComposeRenderEffect()
    }
}