package com.dpfr.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Window content: the prank view plus a border while the item is being placed. */
class ItemHost(context: Context, val prank: View) : FrameLayout(context) {

    var editing: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var picked: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val d = resources.displayMetrics.density
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    init {
        addView(prank, FrameLayout.LayoutParams(-1, -1))
        setWillNotDraw(false)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (!editing) return
        borderPaint.color = if (picked) 0xFF0A84FF.toInt() else 0x99FFFFFF.toInt()
        borderPaint.strokeWidth = (if (picked) 3f else 1.5f) * d
        val inset = 2f * d
        canvas.drawRoundRect(
            RectF(inset, inset, width - inset, height - inset),
            8f * d, 8f * d, borderPaint
        )
    }
}

/**
 * One finger moves the item. Pinch resizes it. A horizontal pinch changes only the width,
 * a vertical pinch only the height, a diagonal pinch both.
 */
class DragResizeTouchHandler(
    private val host: View,
    private val wm: WindowManager,
    private val lp: WindowManager.LayoutParams,
    private val minSize: Int,
    private val maxSize: Int,
    private val onSelect: () -> Unit
) : View.OnTouchListener {

    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false

    private val scaleDetector = ScaleGestureDetector(
        host.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val uniform = detector.scaleFactor
                val px = detector.previousSpanX
                val py = detector.previousSpanY
                val fx = if (px > 40f) detector.currentSpanX / px else 1f
                val fy = if (py > 40f) detector.currentSpanY / py else 1f
                val bothSmall = px <= 40f && py <= 40f
                val sx = if (bothSmall) uniform else fx
                val sy = if (bothSmall) uniform else fy

                val newW = (lp.width * sx).toInt().coerceIn(minSize, maxSize)
                val newH = (lp.height * sy).toInt().coerceIn(minSize, maxSize)
                lp.x -= (newW - lp.width) / 2
                lp.y -= (newH - lp.height) / 2
                lp.width = newW
                lp.height = newH
                apply()
                return true
            }
        }
    )

    private fun apply() {
        runCatching { wm.updateViewLayout(host, lp) }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, e: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onSelect()
                lastX = e.rawX
                lastY = e.rawY
                dragging = true
            }

            MotionEvent.ACTION_POINTER_DOWN -> dragging = false

            MotionEvent.ACTION_MOVE -> {
                if (dragging && e.pointerCount == 1 && !scaleDetector.isInProgress) {
                    lp.x += (e.rawX - lastX).toInt()
                    lp.y += (e.rawY - lastY).toInt()
                    lastX = e.rawX
                    lastY = e.rawY
                    apply()
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (Build.VERSION.SDK_INT >= 29 && e.pointerCount == 2) {
                    val remaining = if (e.actionIndex == 0) 1 else 0
                    lastX = e.getRawX(remaining)
                    lastY = e.getRawY(remaining)
                    dragging = true
                } else {
                    dragging = false
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return true
    }
}

/** Floating bar at the bottom of the screen while any item is in edit mode. */
class EditBar(
    private val context: Context,
    private val wm: WindowManager,
    private val onLess: () -> Unit,
    private val onMore: () -> Unit,
    private val onDelete: () -> Unit,
    private val onDone: () -> Unit
) {
    private val d = context.resources.displayMetrics.density
    private val less = button("Less", Color.WHITE, onLess)
    private val more = button("More", Color.WHITE, onMore)
    private val delete = button("Delete", 0xFFFF453A.toInt(), onDelete)
    private val done = button("Done", 0xFF0A84FF.toInt(), onDone, bold = true)

    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding((8 * d).toInt(), (4 * d).toInt(), (8 * d).toInt(), (4 * d).toInt())
        background = GradientDrawable().apply {
            setColor(0xEB1C1C1E.toInt())
            cornerRadius = 26 * d
        }
        addView(less)
        addView(more)
        addView(delete)
        addView(done)
    }

    private var shown = false

    private fun button(text: String, color: Int, action: () -> Unit, bold: Boolean = false): TextView =
        TextView(context).apply {
            this.text = text
            setTextColor(color)
            textSize = 16f
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            minHeight = (44 * d).toInt()
            minWidth = (56 * d).toInt()
            setPadding((12 * d).toInt(), 0, (12 * d).toInt(), 0)
            contentDescription = text
            setOnClickListener { action() }
        }

    fun show() {
        if (shown) return
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (96 * d).toInt()
        }
        runCatching {
            wm.addView(root, lp)
            shown = true
        }
    }

    fun setAdjustVisible(visible: Boolean) {
        val v = if (visible) View.VISIBLE else View.GONE
        less.visibility = v
        more.visibility = v
    }

    fun hide() {
        if (!shown) return
        runCatching { wm.removeView(root) }
        shown = false
    }
}
