package com.dpfr.app.overlay.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import java.util.Random

/**
 * A thin stuck line, like a broken panel. The window is wider than the line so it is easy
 * to grab while placing. Tall view gives a vertical line, wide view gives a horizontal line.
 */
class DeadPixelView(context: Context) : View(context) {

    private val d = resources.displayMetrics.density
    private val palette = intArrayOf(
        0xFF00FF3C.toInt(), 0xFFFF00E6.toInt(), 0xFFFFFFFF.toInt(),
        0xFF00E5FF.toInt(), 0xFFFF2A2A.toInt()
    )
    private val color = palette[Random().nextInt(palette.size)]
    private val paint = Paint().apply { style = Paint.Style.FILL }
    private var flicker = 1f
    private var animator: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator = ValueAnimator.ofFloat(1f, 0.86f, 1f, 0.93f, 1f).apply {
            duration = 1800L
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                flicker = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val thickness = 3f * d
        val fringe = 1f * d
        val vertical = height >= width

        paint.color = color
        paint.alpha = (255 * flicker).toInt().coerceIn(0, 255)

        val fringePaintAlpha = (110 * flicker).toInt().coerceIn(0, 255)

        if (vertical) {
            val left = (width - thickness) / 2f
            canvas.drawRect(left, 0f, left + thickness, height.toFloat(), paint)
            paint.alpha = fringePaintAlpha
            canvas.drawRect(left - fringe, 0f, left, height.toFloat(), paint)
            canvas.drawRect(left + thickness, 0f, left + thickness + fringe, height.toFloat(), paint)
        } else {
            val top = (height - thickness) / 2f
            canvas.drawRect(0f, top, width.toFloat(), top + thickness, paint)
            paint.alpha = fringePaintAlpha
            canvas.drawRect(0f, top - fringe, width.toFloat(), top, paint)
            canvas.drawRect(0f, top + thickness, width.toFloat(), top + thickness + fringe, paint)
        }
    }
}
