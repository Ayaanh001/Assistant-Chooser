package com.hussain.assistantchooser.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.hussain.assistantchooser.R
import com.hussain.assistantchooser.core.KEY_TILE_OPEN_OVERLAY
import com.hussain.assistantchooser.core.PREFS_NAME
import com.hussain.assistantchooser.main.MainActivity
import com.hussain.assistantchooser.overlay.AssistantOverlayActivity

class QuickLaunchTileService : TileService() {

    override fun onClick() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val openOverlay = prefs.getBoolean(KEY_TILE_OPEN_OVERLAY, true)

        val targetClass = if (openOverlay) AssistantOverlayActivity::class.java else MainActivity::class.java
        val intent = Intent(this, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            Log.e("QuickLaunchTileService", "Failed to launch target activity from tile", e)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "Assistant chooser"
            icon  = android.graphics.drawable.Icon.createWithResource(
                this@QuickLaunchTileService, R.drawable.qs_tile
            )
            updateTile()
        }
    }
}
