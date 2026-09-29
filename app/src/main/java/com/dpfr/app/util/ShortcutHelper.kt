package com.dpfr.app.util

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.dpfr.app.R
import com.dpfr.app.data.Feature
import com.dpfr.app.data.FeatureSettings
import com.dpfr.app.data.ShapeStore
import com.dpfr.app.overlay.PrankRef
import com.dpfr.app.widget.PrankLabels
import com.dpfr.app.widget.QuickIntents

/** Publishes up to three favorite pranks as long-press shortcuts on the app icon. */
object ShortcutHelper {

    fun publish(context: Context) {
        val settings = FeatureSettings(context)
        if (!settings.get(Feature.SHORTCUTS)) {
            runCatching { ShortcutManagerCompat.removeAllDynamicShortcuts(context) }
            return
        }
        val shapes = ShapeStore(context).list()
        val shortcuts = settings.favorites.take(3).mapNotNull { key ->
            val ref = PrankRef.parse(key) ?: return@mapNotNull null
            ShortcutInfoCompat.Builder(context, "prank_" + key.replace(':', '_'))
                .setShortLabel(PrankLabels.short(shapes, ref))
                .setLongLabel(PrankLabels.label(shapes, ref))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_tile))
                .setIntent(QuickIntents.start(context, key, "shortcut"))
                .build()
        }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }
}
