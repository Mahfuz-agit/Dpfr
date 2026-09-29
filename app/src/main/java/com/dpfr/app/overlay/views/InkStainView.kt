package com.dpfr.app.overlay.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.animation.OvershootInterpolator
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** A dark blue ink blot with drips and droplets. It splashes out when it appears. */
class InkStainView(context: Context) : View(context) {

    private val seed = System.nanoTime()
    private val blob = Path()
    private val drips = Path()
    private val drops = ArrayList<FloatArray>()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x22FFFFFF
    }

    private var cx = 0f
    private var cy = 0f
    private var radius = 0f
    private var grow = 0.15f
    private var animator: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator = ValueAnimator.ofFloat(0.15f, 1f).apply {
            duration = 420L
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener {
                grow = it.animatedValue as Float
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        build(w.toFloat(), h.toFloat())
    }

    private fun build(w: Float, h: Float) {
        blob.reset()
        drips.reset()
        drops.clear()
        val rnd = Random(seed)

        cx = w / 2f
        cy = h * 0.42f
        radius = min(w, h) * 0.30f
        val r = radius

        val n = 16
        val pts = ArrayList<PointF>()
        for (i in 0 until n) {
            val angle = (2.0 * PI * i / n).toFloat() + (rnd.nextFloat() - 0.5f) * 0.25f
            val rr = r * (0.72f + rnd.nextFloat() * 0.55f)
            pts.add(PointF(cx + cos(angle) * rr, cy + sin(angle) * rr))
        }
        blob.moveTo((pts[n - 1].x + pts[0].x) / 2f, (pts[n - 1].y + pts[0].y) / 2f)
        for (i in 0 until n) {
            val next = pts[(i + 1) % n]
            blob.quadTo(pts[i].x, pts[i].y, (pts[i].x + next.x) / 2f, (pts[i].y + next.y) / 2f)
        }
        blob.close()

        for (i in 0 until 7) {
            val angle = rnd.nextFloat() * (2f * PI.toFloat())
            val dist = r * (1.25f + rnd.nextFloat() * 0.5f)
            drops.add(
                floatArrayOf(
                    cx + cos(angle) * dist,
                    cy + sin(angle) * dist,
                    r * (0.04f + rnd.nextFloat() * 0.08f)
                )
            )
        }

        for (i in 0 until 3) {
            val x = cx + (rnd.nextFloat() - 0.5f) * r * 1.2f
            val startY = cy + r * 0.6f
            val length = r * (0.5f + rnd.nextFloat() * 0.7f)
            val width = r * 0.09f
            drips.addRoundRect(
                RectF(x - width / 2f, startY, x + width / 2f, startY + length),
                width / 2f, width / 2f, Path.Direction.CW
            )
        }

        fill.shader = RadialGradient(
            cx, cy, r * 1.4f,
            intArrayOf(0xF2080818.toInt(), 0xEE0B0B22.toInt(), 0xCC15153A.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.scale(grow, grow, cx, cy)
        canvas.drawPath(blob, fill)
        canvas.drawPath(drips, fill)
        for (drop in drops) canvas.drawCircle(drop[0], drop[1], drop[2], fill)
        canvas.drawOval(
            cx - radius * 0.45f, cy - radius * 0.5f,
            cx - radius * 0.15f, cy - radius * 0.3f, shine
        )
        canvas.restore()
    }
}
