package com.dpfr.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.appwidget.updateAll
import com.dpfr.app.QuickActionActivity
import com.dpfr.app.data.CustomShape
import com.dpfr.app.data.ShapeStore
import com.dpfr.app.overlay.PrankRef
import com.dpfr.app.overlay.PrankType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Intents that open the invisible [QuickActionActivity]. A visible activity is allowed to start
 * the overlay service on every Android version, so widgets, the tile and shortcuts all use it.
 * The data Uri makes each PendingIntent unique.
 */
object QuickIntents {

    fun start(context: Context, key: String, src: String): Intent =
        Intent(context, QuickActionActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("dpfr://start/$src/${Uri.encode(key)}")
            putExtra(QuickActionActivity.EXTRA_ACTION, "start")
            putExtra(QuickActionActivity.EXTRA_KEY, key)
            putExtra(QuickActionActivity.EXTRA_SRC, src)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun stop(context: Context, src: String): Intent =
        Intent(context, QuickActionActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("dpfr://stop/$src")
            putExtra(QuickActionActivity.EXTRA_ACTION, "stop")
            putExtra(QuickActionActivity.EXTRA_SRC, src)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}

object PrankLabels {

    fun label(shapes: List<CustomShape>, ref: PrankRef): String =
        if (ref.type == PrankType.CUSTOM) {
            shapes.firstOrNull { it.id == ref.shapeId }?.name ?: "Shape"
        } else {
            ref.type.label
        }

    fun short(shapes: List<CustomShape>, ref: PrankRef): String =
        if (ref.type == PrankType.CUSTOM) {
            (shapes.firstOrNull { it.id == ref.shapeId }?.name ?: "Shape").take(7)
        } else {
            ref.type.short
        }

    fun label(context: Context, ref: PrankRef): String = label(ShapeStore(context).list(), ref)
}

object WidgetRefresh {

    /** Redraws both widgets after favorites, the last prank or a switch changed. */
    fun all(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                SmallWidget().updateAll(app)
                MediumWidget().updateAll(app)
            }
        }
    }
}
