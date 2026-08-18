package com.hussain.assistantchooser.core

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserHandle

data class AssistantApp(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val shortcutId: String? = null,
    val userHandle: UserHandle? = null,
    val appName: String? = null,
    val activityName: String? = null,
    val intents: List<Intent>? = null,
    val parentIcon: Drawable? = null
) {
    val isLaunchable: Boolean get() = intents != null || shortcutId == null
    val intent: Intent? get() = intents?.lastOrNull()
    val iconBitmap: Bitmap by lazy { icon.toBitmap() }

    /** Unique key for this app/shortcut to be stored in preferences */
    val key: String get() = buildString {
        append(packageName)
        if (shortcutId != null) {
            append(":")
            append(shortcutId)
        } else if (activityName != null) {
            append("/")
            append(activityName)
        }
        if (userHandle != null) {
            append(":")
            append(userHandle.toString().filter { it.isDigit() })
        }
    }

    fun getThemedIconBitmap(
        backgroundColor: Int,
        foregroundColor: Int
    ): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val adaptiveIcon = icon as? AdaptiveIconDrawable ?: return null
        val monochrome = adaptiveIcon.monochrome ?: return null

        return runCatching {
            val bg = ColorDrawable(backgroundColor)
            val fg = monochrome.constantState?.newDrawable()?.mutate()
            fg?.setTint(foregroundColor)

            val themedIcon = AdaptiveIconDrawable(bg, fg)
            themedIcon.toBitmap()
        }.getOrNull()
    }
}

fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) return this.bitmap
    
    val isAdaptive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this is AdaptiveIconDrawable
    val w = intrinsicWidth.coerceAtLeast(1)
    val h = intrinsicHeight.coerceAtLeast(1)

    return if (isAdaptive) {
        // Adaptive icons have a 108x108 area with a 72x72 safe zone.
        // 1.0f = Full Bleed (Default Android behavior, no cropping)
        val cropFactor = 1.0f
        val safeW = (w * cropFactor).toInt()
        val safeH = (h * cropFactor).toInt()
        val bmp = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val offsetW = (safeW - w) / 2
        val offsetH = (safeH - h) / 2
        setBounds(offsetW, offsetH, offsetW + w, offsetH + h)
        draw(canvas)
        bmp
    } else {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        setBounds(0, 0, w, h)
        draw(canvas)
        bmp
    }
}
