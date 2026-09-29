package com.dpfr.app.tile

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.dpfr.app.data.Feature
import com.dpfr.app.data.FeatureSettings
import com.dpfr.app.overlay.OverlayService
import com.dpfr.app.overlay.PrankType
import com.dpfr.app.widget.QuickIntents

/** One tap: stop everything if a prank is running, otherwise start the last prank. */
class DpfrTileService : TileService() {

    companion object {
        fun refresh(context: Context) {
            runCatching {
                TileService.requestListeningState(
                    context,
                    ComponentName(context, DpfrTileService::class.java)
                )
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val settings = FeatureSettings(this)
        if (!settings.get(Feature.TILE)) {
            updateTile()
            return
        }
        val intent = if (OverlayService.running) {
            QuickIntents.stop(this, "tile")
        } else {
            val key = settings.lastPrank
                ?: settings.favorites.firstOrNull()
                ?: PrankType.CRACKED_1.name
            QuickIntents.start(this, key, "tile")
        }
        launch(intent)
    }

    private fun launch(intent: Intent) {
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val enabled = FeatureSettings(this).get(Feature.TILE)
        tile.label = "Dpfr"
        tile.state = when {
            !enabled -> Tile.STATE_UNAVAILABLE
            OverlayService.running -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = when {
                !enabled -> "Off in settings"
                OverlayService.running -> "Tap to stop"
                else -> "Tap to start"
            }
        }
        tile.updateTile()
    }
}
