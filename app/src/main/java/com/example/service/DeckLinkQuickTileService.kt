package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity

/**
 * Android Quick Settings Tile allowing one-tap launch into DeckLink Hotspot/HID Quick Connect
 * directly from the system notification shade.
 */
class DeckLinkQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        syncTileAppearance()
    }

    override fun onClick() {
        super.onClick()
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_QUICK_ACTION, QUICK_ACTION_TOGGLE_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                4201,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(launchIntent)
        }
    }

    private fun syncTileAppearance() {
        val tile = qsTile ?: return
        tile.label = "DeckLink PC"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (lastKnownConnected) "Connected" else "Tap to Connect"
        }
        tile.state = if (lastKnownConnected) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    companion object {
        const val EXTRA_QUICK_ACTION = "decklink_quick_action"
        const val QUICK_ACTION_TOGGLE_CONNECT = "toggle_connect"
        const val QUICK_ACTION_HOTSPOT = "start_hotspot"
        const val QUICK_ACTION_PLAY_PAUSE = "media_play_pause"
        const val QUICK_ACTION_MUTE = "media_mute"
        const val QUICK_ACTION_LOCK_PC = "lock_pc"

        @Volatile
        var lastKnownConnected: Boolean = false
    }
}
