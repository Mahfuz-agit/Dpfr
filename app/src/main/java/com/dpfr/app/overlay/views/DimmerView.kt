package com.dpfr.app.overlay.views

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.dpfr.app.data.FeatureSettings
import com.dpfr.app.overlay.Adjustable
import kotlin.math.min

/**
 * Darkens one region with soft edges. This is a fake dimmer: a black layer on top of the
 * screen. The real backlight does not change, so battery is not saved.
 */
class DimmerView(context: Context) : View(context), Adjustable {

    private val d = resources.displayMetrics.density
    private val settings = FeatureSettings(context)
    private val featherDp = settings.dimmerFeatherDp

    override var intensity: Float = settings.dimmerIntensity
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }
    private var inset = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val feather = min(featherDp * d, min(w, h) / 6f)
        if (feather >= 1f) {
            paint.maskFilter = BlurMaskFilter(feather, BlurMaskFilter.Blur.NORMAL)
            inset = feather * 1.3f
        } else {
            paint.maskFilter = null
            inset = 0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        paint.alpha = (255 * intensity).toInt().coerceIn(0, 255)
        val rect = RectF(inset, inset, width - inset, height - inset)
        if (rect.width() <= 0f || rect.height() <= 0f) return
        val radius = min(28f * d, min(rect.width(), rect.height()) / 2f)
        canvas.drawRoundRect(rect, radius, radius, paint)
    }
}
