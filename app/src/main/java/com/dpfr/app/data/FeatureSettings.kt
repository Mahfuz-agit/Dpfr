package com.dpfr.app.data

import android.content.Context
import android.content.SharedPreferences
import com.dpfr.app.overlay.PrankRef

enum class FeatureGroup(val title: String) {
    PRANK("Pranks"),
    TOOL("Tools"),
    ACCESS("Quick access")
}

/** Every feature in Dpfr has its own on/off switch. */
enum class Feature(
    val key: String,
    val title: String,
    val subtitle: String,
    val default: Boolean,
    val group: FeatureGroup
) {
    CRACKED("f_cracked", "Cracked screen", "Three glass-crack styles", true, FeatureGroup.PRANK),
    BUGS("f_bugs", "Bugs", "Cockroach, beetle and ants crawl on the screen", true, FeatureGroup.PRANK),
    INK("f_ink", "Ink stain", "A fake ink blot with drips", true, FeatureGroup.PRANK),
    DEAD_PIXEL("f_deadpixel", "Dead pixel line", "A stuck colored line", true, FeatureGroup.PRANK),
    DIMMER("f_dimmer", "Area dimmer", "Darken one region of the screen", true, FeatureGroup.PRANK),
    CUSTOM("f_custom", "Custom shapes", "Draw and place your own shapes", true, FeatureGroup.PRANK),

    SHAKE_STOP("f_shake", "Shake to stop", "Shake the phone to remove all pranks", true, FeatureGroup.TOOL),
    AUTO_STOP("f_autostop", "Auto stop timer", "Remove pranks after a delay", true, FeatureGroup.TOOL),
    HAPTICS("f_haptics", "Haptic feedback", "Small vibration on taps", true, FeatureGroup.TOOL),
    APP_TRIGGER("f_trigger", "App trigger", "Run a prank when a chosen app opens", false, FeatureGroup.TOOL),

    WIDGETS("f_widgets", "Home widgets", "Small and medium one-tap widgets", true, FeatureGroup.ACCESS),
    TILE("f_tile", "Quick Settings tile", "One-tap tile in the notification shade", true, FeatureGroup.ACCESS),
    SHORTCUTS("f_shortcuts", "Icon shortcuts", "Long-press the app icon for favorites", true, FeatureGroup.ACCESS)
}

class FeatureSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("dpfr_settings", Context.MODE_PRIVATE)

    fun get(f: Feature): Boolean = prefs.getBoolean(f.key, f.default)

    fun set(f: Feature, value: Boolean) {
        prefs.edit().putBoolean(f.key, value).apply()
    }

    fun isPrankEnabled(ref: PrankRef): Boolean = get(ref.type.feature)

    var autoStopSeconds: Int
        get() = prefs.getInt("auto_stop_sec", 60)
        set(value) {
            prefs.edit().putInt("auto_stop_sec", value).apply()
        }

    var dimmerIntensity: Float
        get() = prefs.getFloat("dim_intensity", 0.6f)
        set(value) {
            prefs.edit().putFloat("dim_intensity", value).apply()
        }

    var dimmerFeatherDp: Float
        get() = prefs.getFloat("dim_feather", 24f)
        set(value) {
            prefs.edit().putFloat("dim_feather", value).apply()
        }

    var lastPrank: String?
        get() = prefs.getString("last_prank", null)
        set(value) {
            prefs.edit().putString("last_prank", value).apply()
        }

    /** Up to four prank keys shown in the medium widget, tile and shortcuts. */
    var favorites: List<String>
        get() {
            val raw = prefs.getString("favorites", null) ?: return DEFAULT_FAVORITES
            return raw.split("|").filter { it.isNotBlank() }
        }
        set(value) {
            prefs.edit().putString("favorites", value.joinToString("|")).apply()
        }

    /** Fractions of the screen: left, top, width, height. */
    fun lastPlacement(key: String): FloatArray? {
        val raw = prefs.getString("place_$key", null) ?: return null
        val parts = raw.split(",")
        if (parts.size != 4) return null
        return runCatching { FloatArray(4) { parts[it].toFloat() } }.getOrNull()
    }

    fun setLastPlacement(key: String, value: FloatArray) {
        prefs.edit().putString("place_$key", value.joinToString(",")).apply()
    }

    companion object {
        val DEFAULT_FAVORITES = listOf("CRACKED_1", "BUG_COCKROACH", "INK", "DIMMER")
    }
}
