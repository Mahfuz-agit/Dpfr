package com.dpfr.app.overlay.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.view.View
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Fake glass cracks. Variant 1: one big impact. Variant 2: impact near a corner.
 * Variant 3: three smaller impacts. The layout is random each time but stays the same
 * while the view is moved or resized.
 */
class CrackedScreenView(context: Context, private val variant: Int) : View(context) {

    private val d = resources.displayMetrics.density
    private val seed = System.nanoTime()

    private val thick = Path()
    private val thin = Path()
    private val shards = Path()

    private val shadowPaint = strokePaint(0x99000000.toInt(), 3.2f)
    private val glowPaint = strokePaint(0x33FFFFFF, 5f)
    private val linePaint = strokePaint(0xF2FFFFFF.toInt(), 1.7f)
    private val finePaint = strokePaint(0xCCFFFFFF.toInt(), 0.9f)
    private val shardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x24FFFFFF
    }

    private fun strokePaint(color: Int, widthDp: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        this.color = color
        strokeWidth = widthDp * d
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        build(w.toFloat(), h.toFloat())
    }

    private fun build(w: Float, h: Float) {
        thick.reset()
        thin.reset()
        shards.reset()
        val rnd = Random(seed)
        val reach = max(w, h)
        when (variant) {
            1 -> impact(rnd, w * 0.5f, h * 0.42f, 13, reach * 0.95f)
            2 -> impact(rnd, w * 0.84f, h * 0.10f, 10, reach * 0.80f)
            else -> {
                impact(rnd, w * 0.28f, h * 0.30f, 9, reach * 0.50f)
                impact(rnd, w * 0.72f, h * 0.55f, 10, reach * 0.60f)
                impact(rnd, w * 0.40f, h * 0.85f, 7, reach * 0.40f)
            }
        }
    }

    private fun impact(rnd: Random, cx: Float, cy: Float, arms: Int, reach: Float) {
        val rings = Array(arms) { ArrayList<PointF>() }

        for (i in 0 until arms) {
            var angle = (2.0 * PI * i / arms).toFloat() + (rnd.nextFloat() - 0.5f) * 0.4f
            val length = reach * (0.4f + rnd.nextFloat() * 0.6f)
            var dist = 0f
            rings[i].add(PointF(cx, cy))
            thick.moveTo(cx, cy)
            while (dist < length) {
                dist += (10f + rnd.nextFloat() * 26f) * d
                angle += (rnd.nextFloat() - 0.5f) * 0.24f
                val px = cx + cos(angle) * dist
                val py = cy + sin(angle) * dist
                thick.lineTo(px, py)
                rings[i].add(PointF(px, py))
                if (rnd.nextFloat() < 0.28f && dist > 30f * d) {
                    val side = if (rnd.nextBoolean()) 0.7f else -0.7f
                    branch(rnd, px, py, angle + side, (30f + rnd.nextFloat() * 90f) * d)
                }
            }
        }

        val maxIndex = rings.minOf { it.size } - 1
        var r = 2
        while (r <= min(maxIndex, 7)) {
            for (i in 0 until arms) {
                val a = rings[i][r]
                val b = rings[(i + 1) % arms][r]
                val mx = (a.x + b.x) / 2f + (rnd.nextFloat() - 0.5f) * 14f * d
                val my = (a.y + b.y) / 2f + (rnd.nextFloat() - 0.5f) * 14f * d
                thin.moveTo(a.x, a.y)
                thin.lineTo(mx, my)
                thin.lineTo(b.x, b.y)
            }
            r += 2
        }

        val pieces = 9
        for (k in 0 until pieces) {
            val radius = (8f + rnd.nextFloat() * 16f) * d
            val a = (2.0 * PI * k / pieces).toFloat()
            val x = cx + cos(a) * radius
            val y = cy + sin(a) * radius
            if (k == 0) shards.moveTo(x, y) else shards.lineTo(x, y)
        }
        shards.close()
    }

    private fun branch(rnd: Random, x: Float, y: Float, angle: Float, length: Float) {
        var a = angle
        var px = x
        var py = y
        var dist = 0f
        thin.moveTo(px, py)
        while (dist < length) {
            val step = (8f + rnd.nextFloat() * 14f) * d
            dist += step
            a += (rnd.nextFloat() - 0.5f) * 0.3f
            px += cos(a) * step
            py += sin(a) * step
            thin.lineTo(px, py)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(1.5f * d, 1.5f * d)
        canvas.drawPath(thick, shadowPaint)
        canvas.drawPath(thin, shadowPaint)
        canvas.restore()

        canvas.drawPath(thick, glowPaint)
        canvas.drawPath(thick, linePaint)
        canvas.drawPath(thin, finePaint)
        canvas.drawPath(shards, shardPaint)
    }
}
