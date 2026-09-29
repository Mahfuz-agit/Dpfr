package com.dpfr.app.data

import android.graphics.Color
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin

enum class ShapeKind(val label: String) {
    CIRCLE("Circle"),
    RECT("Rectangle"),
    TRIANGLE("Triangle"),
    STAR("Star"),
    HEART("Heart"),
    FREEHAND("Freehand")
}

/**
 * A user-made shape. [points] are normalized 0..1 inside the shape bounding box
 * and are only used by [ShapeKind.FREEHAND].
 */
data class CustomShape(
    val id: String,
    val name: String,
    val kind: ShapeKind,
    val points: List<PointF>,
    val aspect: Float,
    val fillColor: Int,
    val opacityPct: Int,
    val blurDp: Float,
    val borderColor: Int,
    val borderWidthDp: Float
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("kind", kind.name)
        put("aspect", aspect.toDouble())
        put("fill", fillColor)
        put("opacity", opacityPct)
        put("blur", blurDp.toDouble())
        put("border", borderColor)
        put("borderW", borderWidthDp.toDouble())
        val arr = JSONArray()
        points.forEach {
            arr.put(it.x.toDouble())
            arr.put(it.y.toDouble())
        }
        put("points", arr)
    }

    companion object {
        fun fromJson(o: JSONObject): CustomShape {
            val arr = o.optJSONArray("points") ?: JSONArray()
            val pts = ArrayList<PointF>()
            var i = 0
            while (i + 1 < arr.length()) {
                pts.add(PointF(arr.getDouble(i).toFloat(), arr.getDouble(i + 1).toFloat()))
                i += 2
            }
            val kind = runCatching { ShapeKind.valueOf(o.getString("kind")) }
                .getOrDefault(ShapeKind.CIRCLE)
            return CustomShape(
                id = o.getString("id"),
                name = o.optString("name", "Shape"),
                kind = kind,
                points = pts,
                aspect = o.optDouble("aspect", 1.0).toFloat(),
                fillColor = o.optInt("fill", Color.BLACK),
                opacityPct = o.optInt("opacity", 85),
                blurDp = o.optDouble("blur", 0.0).toFloat(),
                borderColor = o.optInt("border", Color.WHITE),
                borderWidthDp = o.optDouble("borderW", 0.0).toFloat()
            )
        }
    }
}

object ShapeGeometry {

    /** Builds the outline of a shape that fills a [w] x [h] box. */
    fun buildPath(kind: ShapeKind, points: List<PointF>, w: Float, h: Float): Path {
        val p = Path()
        when (kind) {
            ShapeKind.CIRCLE -> p.addOval(RectF(0f, 0f, w, h), Path.Direction.CW)

            ShapeKind.RECT -> {
                val r = minOf(w, h) * 0.12f
                p.addRoundRect(RectF(0f, 0f, w, h), r, r, Path.Direction.CW)
            }

            ShapeKind.TRIANGLE -> {
                p.moveTo(w / 2f, 0f)
                p.lineTo(w, h)
                p.lineTo(0f, h)
                p.close()
            }

            ShapeKind.STAR -> {
                val cx = w / 2f
                val cy = h / 2f
                val rx = w / 2f
                val ry = h / 2f
                for (i in 0 until 10) {
                    val radiusFactor = if (i % 2 == 0) 1f else 0.42f
                    val angle = Math.toRadians((-90 + i * 36).toDouble())
                    val x = cx + (cos(angle) * rx * radiusFactor).toFloat()
                    val y = cy + (sin(angle) * ry * radiusFactor).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
            }

            ShapeKind.HEART -> {
                p.moveTo(0.5f * w, 0.95f * h)
                p.cubicTo(0.10f * w, 0.65f * h, 0.00f * w, 0.40f * h, 0.00f * w, 0.28f * h)
                p.cubicTo(0.00f * w, 0.10f * h, 0.15f * w, 0.00f * h, 0.28f * w, 0.00f * h)
                p.cubicTo(0.40f * w, 0.00f * h, 0.47f * w, 0.08f * h, 0.50f * w, 0.18f * h)
                p.cubicTo(0.53f * w, 0.08f * h, 0.60f * w, 0.00f * h, 0.72f * w, 0.00f * h)
                p.cubicTo(0.85f * w, 0.00f * h, 1.00f * w, 0.10f * h, 1.00f * w, 0.28f * h)
                p.cubicTo(1.00f * w, 0.40f * h, 0.90f * w, 0.65f * h, 0.5f * w, 0.95f * h)
                p.close()
            }

            ShapeKind.FREEHAND -> {
                val n = points.size
                if (n >= 3) {
                    fun px(i: Int) = points[i].x * w
                    fun py(i: Int) = points[i].y * h
                    p.moveTo((px(n - 1) + px(0)) / 2f, (py(n - 1) + py(0)) / 2f)
                    for (i in 0 until n) {
                        val j = (i + 1) % n
                        p.quadTo(px(i), py(i), (px(i) + px(j)) / 2f, (py(i) + py(j)) / 2f)
                    }
                    p.close()
                }
            }
        }
        return p
    }
}
