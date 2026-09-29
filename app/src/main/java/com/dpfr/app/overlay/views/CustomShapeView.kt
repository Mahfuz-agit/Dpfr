package com.dpfr.app.overlay.views

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.dpfr.app.data.CustomShape
import com.dpfr.app.data.ShapeGeometry
import com.dpfr.app.overlay.Adjustable

/** Draws a [CustomShape] with fill, opacity, blur and border. Used on screen and in previews. */
class CustomShapeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), Adjustable {

    private val d = resources.displayMetrics.density

    var shape: CustomShape? = null
        set(value) {
            field = value
            setLayerType(
                if ((value?.blurDp ?: 0f) > 0f) LAYER_TYPE_SOFTWARE else LAYER_TYPE_NONE,
                null
            )
            invalidate()
        }

    override var intensity: Float = 1f
        set(value) {
            field = value
            invalidate()
        }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        val s = shape ?: return
        val blurPx = s.blurDp * d
        val borderPx = s.borderWidthDp * d
        val inset = borderPx / 2f + blurPx * 1.5f + 1f
        val w = width - inset * 2f
        val h = height - inset * 2f
        if (w <= 0f || h <= 0f) return

        val path = ShapeGeometry.buildPath(s.kind, s.points, w, h)
        val alpha = (255 * (s.opacityPct / 100f) * intensity).toInt().coerceIn(0, 255)

        canvas.save()
        canvas.translate(inset, inset)

        fillPaint.color = s.fillColor
        fillPaint.alpha = alpha
        fillPaint.maskFilter =
            if (blurPx > 0f) BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL) else null
        canvas.drawPath(path, fillPaint)

        if (borderPx > 0f) {
            borderPaint.color = s.borderColor
            borderPaint.alpha = alpha
            borderPaint.strokeWidth = borderPx
            borderPaint.maskFilter =
                if (blurPx > 0f) BlurMaskFilter(blurPx * 0.5f, BlurMaskFilter.Blur.NORMAL) else null
            canvas.drawPath(path, borderPaint)
        }
        canvas.restore()
    }
}
