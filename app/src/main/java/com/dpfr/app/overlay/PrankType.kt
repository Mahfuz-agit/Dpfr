package com.dpfr.app.overlay

import android.graphics.Rect
import com.dpfr.app.data.CustomShape
import com.dpfr.app.data.Feature

enum class PrankType(
    val label: String,
    val short: String,
    val hint: String,
    val feature: Feature
) {
    CRACKED_1("Cracked screen: spider", "Crack", "Big impact in the middle", Feature.CRACKED),
    CRACKED_2("Cracked screen: corner hit", "Corner", "Impact near a corner", Feature.CRACKED),
    CRACKED_3("Cracked screen: multi impact", "Shatter", "Three smaller impacts", Feature.CRACKED),
    BUG_COCKROACH("Cockroach", "Roach", "Fast, stops and dashes", Feature.BUGS),
    BUG_BEETLE("Beetle", "Beetle", "Slow and shiny", Feature.BUGS),
    BUG_ANT("Ants", "Ants", "A small crowd", Feature.BUGS),
    INK("Ink stain", "Ink", "Blot with drips", Feature.INK),
    DEAD_PIXEL("Dead pixel line", "Line", "Thin stuck color line", Feature.DEAD_PIXEL),
    DIMMER("Area dimmer", "Dim", "Darken one region", Feature.DIMMER),
    CUSTOM("Custom shape", "Shape", "Your own shape", Feature.CUSTOM)
}

/** A prank type plus, for custom shapes, the shape id. */
data class PrankRef(val type: PrankType, val shapeId: String? = null) {

    val key: String
        get() = if (type == PrankType.CUSTOM) "CUSTOM:$shapeId" else type.name

    companion object {
        fun parse(key: String): PrankRef? {
            if (key.startsWith("CUSTOM:")) {
                val id = key.removePrefix("CUSTOM:")
                return if (id.isBlank()) null else PrankRef(PrankType.CUSTOM, id)
            }
            return runCatching { PrankRef(PrankType.valueOf(key)) }.getOrNull()
        }
    }
}

data class PrankOption(
    val key: String,
    val label: String,
    val hint: String,
    val feature: Feature
)

object PrankOptions {
    fun all(shapes: List<CustomShape>): List<PrankOption> {
        val builtIn = PrankType.entries
            .filter { it != PrankType.CUSTOM }
            .map { PrankOption(it.name, it.label, it.hint, it.feature) }
        val custom = shapes.map {
            PrankOption("CUSTOM:${it.id}", it.name, "Custom shape", Feature.CUSTOM)
        }
        return builtIn + custom
    }
}

object PrankDefaults {

    /** Default window rectangle in pixels for a prank on a [sw] x [sh] screen. */
    fun rect(type: PrankType, sw: Int, sh: Int, density: Float, aspect: Float): Rect {
        val minDim = minOf(sw, sh)
        return when (type) {
            PrankType.CRACKED_1,
            PrankType.CRACKED_2,
            PrankType.CRACKED_3,
            PrankType.BUG_COCKROACH,
            PrankType.BUG_BEETLE,
            PrankType.BUG_ANT -> Rect(0, 0, sw, sh)

            PrankType.INK -> {
                val side = (minDim * 0.45f).toInt()
                centered(sw, sh, side, side)
            }

            PrankType.DEAD_PIXEL -> {
                val w = (48 * density).toInt()
                val left = (sw * 0.62f).toInt()
                Rect(left, 0, left + w, sh)
            }

            PrankType.DIMMER -> centered(sw, sh, (sw * 0.7f).toInt(), (sh * 0.25f).toInt())

            PrankType.CUSTOM -> {
                val side = minDim * 0.45f
                val w = if (aspect >= 1f) side else side * aspect
                val h = if (aspect >= 1f) side / aspect else side
                centered(sw, sh, w.toInt(), h.toInt())
            }
        }
    }

    private fun centered(sw: Int, sh: Int, w: Int, h: Int): Rect {
        val left = (sw - w) / 2
        val top = (sh - h) / 2
        return Rect(left, top, left + w, top + h)
    }

    /** Scales [base] around a center given as fractions of the screen. */
    fun applyPlacement(base: Rect, cx: Float, cy: Float, scale: Float, sw: Int, sh: Int): Rect {
        val w = (base.width() * scale).toInt().coerceAtLeast(8)
        val h = (base.height() * scale).toInt().coerceAtLeast(8)
        val left = (cx * sw - w / 2f).toInt()
        val top = (cy * sh - h / 2f).toInt()
        return Rect(left, top, left + w, top + h)
    }

    /** [f] is left, top, width, height as fractions of the screen. */
    fun fromFractions(f: FloatArray, sw: Int, sh: Int): Rect {
        val left = (f[0] * sw).toInt()
        val top = (f[1] * sh).toInt()
        val w = (f[2] * sw).toInt().coerceAtLeast(8)
        val h = (f[3] * sh).toInt().coerceAtLeast(8)
        return Rect(left, top, left + w, top + h)
    }
}
