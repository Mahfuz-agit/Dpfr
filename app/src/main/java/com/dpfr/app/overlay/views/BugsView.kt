package com.dpfr.app.overlay.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import java.util.Random
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class BugKind { COCKROACH, BEETLE, ANT }

/** Animated bugs that wander inside the view. Drawn with paths only, no images. */
class BugsView(context: Context, private val kind: BugKind) : View(context) {

    private class Bug(
        var x: Float,
        var y: Float,
        var heading: Float,
        val speed: Float,
        var phase: Float,
        var turnTimer: Float,
        var pause: Float,
        val scale: Float
    )

    private val d = resources.displayMetrics.density
    private val rnd = Random()
    private val bugs = ArrayList<Bug>()
    private var lastFrame = 0L

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val legPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val legPath = Path()

    private val ticker = object : Runnable {
        override fun run() {
            step()
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastFrame = 0L
        postOnAnimation(ticker)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (bugs.isEmpty()) spawn(w, h)
    }

    private fun spawn(w: Int, h: Int) {
        val count = when (kind) {
            BugKind.COCKROACH -> 2
            BugKind.BEETLE -> 3
            BugKind.ANT -> 6
        }
        for (i in 0 until count) {
            val speed = when (kind) {
                BugKind.COCKROACH -> 140f + rnd.nextFloat() * 80f
                BugKind.BEETLE -> 40f + rnd.nextFloat() * 30f
                BugKind.ANT -> 60f + rnd.nextFloat() * 50f
            }
            val scale = (if (kind == BugKind.ANT) 0.8f else 1f) * (0.9f + rnd.nextFloat() * 0.2f)
            val x = w * (0.15f + rnd.nextFloat() * 0.7f)
            val y = h * (0.15f + rnd.nextFloat() * 0.7f)
            val heading = rnd.nextFloat() * (2f * PI.toFloat())
            bugs.add(Bug(x, y, heading, speed, rnd.nextFloat() * 6f, 0.5f + rnd.nextFloat() * 1.5f, 0f, scale))
        }
    }

    private fun step() {
        val now = System.nanoTime()
        val dt = if (lastFrame == 0L) 0.016f else ((now - lastFrame) / 1_000_000_000f).coerceAtMost(0.05f)
        lastFrame = now
        if (width == 0 || height == 0) return

        val margin = 24f * d
        for (b in bugs) {
            if (b.pause > 0f) {
                b.pause -= dt
                continue
            }
            b.phase += dt * (8f + b.speed * 0.08f)
            b.turnTimer -= dt
            if (b.turnTimer <= 0f) {
                b.turnTimer = 0.6f + rnd.nextFloat() * 1.8f
                b.heading += (rnd.nextFloat() - 0.5f) * 1.6f
                if (kind == BugKind.COCKROACH && rnd.nextFloat() < 0.25f) {
                    b.pause = 0.3f + rnd.nextFloat() * 0.9f
                }
            }
            val v = b.speed * d * dt
            b.x += sin(b.heading) * v
            b.y -= cos(b.heading) * v

            if (b.x < margin || b.x > width - margin || b.y < margin || b.y > height - margin) {
                val target = atan2(width / 2f - b.x, -(height / 2f - b.y))
                var diff = target - b.heading
                while (diff > PI) diff -= (2 * PI).toFloat()
                while (diff < -PI) diff += (2 * PI).toFloat()
                b.heading += diff * min(1f, 6f * dt)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        for (b in bugs) {
            canvas.save()
            canvas.translate(b.x, b.y)
            canvas.rotate(Math.toDegrees(b.heading.toDouble()).toFloat())
            canvas.scale(b.scale * d, b.scale * d)
            when (kind) {
                BugKind.COCKROACH -> drawRoach(canvas, b)
                BugKind.BEETLE -> drawBeetle(canvas, b)
                BugKind.ANT -> drawAnt(canvas, b)
            }
            canvas.restore()
        }
    }

    // All drawing below uses dp units. The bug faces up (negative y).

    private fun ellipse(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, color: Int) {
        fill.color = color
        c.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, fill)
    }

    private fun legs(
        c: Canvas,
        b: Bug,
        attachY: FloatArray,
        reach: Float,
        color: Int,
        width: Float
    ) {
        legPaint.color = color
        legPaint.strokeWidth = width
        for (side in intArrayOf(-1, 1)) {
            for (i in attachY.indices) {
                val swing = sin(b.phase + i * 2.1f + (if (side > 0) PI.toFloat() else 0f)) * 5f
                val ay = attachY[i]
                val kneeX = side * reach * 0.62f
                val kneeY = ay + (i - 1) * 4f + swing * 0.6f
                val footX = side * reach
                val footY = ay + (i - 1) * 10f + swing
                legPath.reset()
                legPath.moveTo(side * 5f, ay)
                legPath.lineTo(kneeX, kneeY)
                legPath.lineTo(footX, footY)
                c.drawPath(legPath, legPaint)
            }
        }
    }

    private fun antennae(c: Canvas, b: Bug, baseY: Float, length: Float, color: Int) {
        legPaint.color = color
        legPaint.strokeWidth = 1f
        val wiggle = sin(b.phase * 0.7f) * 4f
        for (side in intArrayOf(-1, 1)) {
            legPath.reset()
            legPath.moveTo(side * 2f, baseY)
            legPath.quadTo(side * (length * 0.3f) + wiggle, baseY - length * 0.6f, side * (length * 0.6f), baseY - length)
            c.drawPath(legPath, legPaint)
        }
    }

    private fun drawRoach(c: Canvas, b: Bug) {
        ellipse(c, 2f, 11f, 9f, 18f, 0x33000000)
        legs(c, b, floatArrayOf(-8f, 0f, 8f), 22f, 0xFF2F1A0B.toInt(), 1.6f)
        antennae(c, b, -19f, 26f, 0xFF2F1A0B.toInt())
        ellipse(c, 0f, 8f, 9f, 18f, 0xFF5E3A1C.toInt())
        fill.color = 0x33FFFFFF
        c.drawRect(-0.7f, -6f, 0.7f, 24f, fill)
        ellipse(c, 0f, -6f, 7f, 9f, 0xFF4A2B14.toInt())
        ellipse(c, 0f, -17f, 4.5f, 4.5f, 0xFF3A200E.toInt())
        ellipse(c, -3f, 2f, 2f, 7f, 0x22FFFFFF)
    }

    private fun drawBeetle(c: Canvas, b: Bug) {
        ellipse(c, 2f, 6f, 12f, 13f, 0x33000000)
        legs(c, b, floatArrayOf(-6f, 2f, 10f), 16f, 0xFF0B0E11.toInt(), 1.8f)
        antennae(c, b, -12f, 12f, 0xFF0B0E11.toInt())
        ellipse(c, 0f, 4f, 11f, 13f, 0xFF14181C.toInt())
        fill.color = 0x55000000
        c.drawRect(-0.6f, -8f, 0.6f, 17f, fill)
        ellipse(c, -4f, -1f, 3f, 7f, 0x44FFFFFF)
        ellipse(c, 0f, -10f, 5f, 4.5f, 0xFF0B0E11.toInt())
    }

    private fun drawAnt(c: Canvas, b: Bug) {
        ellipse(c, 1.5f, 5f, 6f, 13f, 0x33000000)
        legs(c, b, floatArrayOf(-8f, -2f, 4f), 20f, 0xFF1B100B.toInt(), 1.1f)
        antennae(c, b, -15f, 14f, 0xFF1B100B.toInt())
        ellipse(c, 0f, 8f, 5.5f, 8.5f, 0xFF2B1B14.toInt())
        ellipse(c, 0f, -5f, 3.5f, 5f, 0xFF2B1B14.toInt())
        ellipse(c, 0f, -13f, 4f, 4f, 0xFF2B1B14.toInt())
        ellipse(c, -1.5f, 6f, 1.5f, 4f, 0x33FFFFFF)
    }
}
