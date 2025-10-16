package com.hussain.assistantchooser

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class QuickLaunchTileService : TileService() {

    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            Log.e("QuickLaunchTileService", "Failed to launch MainActivity from tile", e)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "Launch Assistant"
            icon = android.graphics.drawable.Icon.createWithResource(this@QuickLaunchTileService, R.drawable.qs_tile)
            updateTile()
        }
    }

}
