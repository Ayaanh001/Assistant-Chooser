package com.hussain.assistantchooser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

data class AssistantApp(
    val name: String,
    val packageName: String,
    val icon: Drawable
) {
    // Cache the bitmap conversion
    val iconBitmap: Bitmap by lazy { icon.toBitmap() }
}

// Drawable → Bitmap helper
fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) return this.bitmap
    val width = intrinsicWidth.takeIf { it > 0 } ?: 1
    val height = intrinsicHeight.takeIf { it > 0 } ?: 1
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bmp
}