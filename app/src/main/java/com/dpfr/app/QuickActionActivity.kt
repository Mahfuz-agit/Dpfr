package com.dpfr.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.dpfr.app.data.Feature
import com.dpfr.app.data.FeatureSettings
import com.dpfr.app.overlay.OverlayService
import com.dpfr.app.overlay.PrankLauncher

/**
 * Invisible trampoline. Widgets, the Quick Settings tile and icon shortcuts open it.
 * It checks the feature switch, does the action, and closes at once.
 */
class QuickActionActivity : Activity() {

    companion object {
        const val EXTRA_ACTION = "dpfr_action"
        const val EXTRA_KEY = "dpfr_key"
        const val EXTRA_SRC = "dpfr_src"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
        finish()
    }

    private fun handle(intent: Intent) {
        val settings = FeatureSettings(this)
        val gate = when (intent.getStringExtra(EXTRA_SRC)) {
            "widget" -> Feature.WIDGETS
            "tile" -> Feature.TILE
            "shortcut" -> Feature.SHORTCUTS
            else -> null
        }
        if (gate != null && !settings.get(gate)) {
            Toast.makeText(this, "${gate.title} is turned off in Dpfr settings", Toast.LENGTH_SHORT).show()
            return
        }
        when (intent.getStringExtra(EXTRA_ACTION)) {
            "start" -> {
                val key = intent.getStringExtra(EXTRA_KEY) ?: return
                PrankLauncher.launch(this, key, edit = false)
            }
            "stop" -> OverlayService.stopAll(this)
        }
    }
}
