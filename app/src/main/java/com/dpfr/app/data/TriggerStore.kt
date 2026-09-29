package com.dpfr.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * One rule: when [packageName] comes to the foreground, show [prankKey].
 * Position is the prank center in percent of the screen, scale is percent of default size.
 */
data class TriggerRule(
    val id: String,
    val packageName: String,
    val appLabel: String,
    val prankKey: String,
    val enabled: Boolean,
    val xPct: Int,
    val yPct: Int,
    val scalePct: Int
) {
    val placement: FloatArray
        get() = floatArrayOf(xPct / 100f, yPct / 100f, scalePct / 100f)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("pkg", packageName)
        put("label", appLabel)
        put("prank", prankKey)
        put("enabled", enabled)
        put("x", xPct)
        put("y", yPct)
        put("scale", scalePct)
    }

    companion object {
        fun fromJson(o: JSONObject) = TriggerRule(
            id = o.getString("id"),
            packageName = o.getString("pkg"),
            appLabel = o.optString("label", o.getString("pkg")),
            prankKey = o.getString("prank"),
            enabled = o.optBoolean("enabled", true),
            xPct = o.optInt("x", 50),
            yPct = o.optInt("y", 50),
            scalePct = o.optInt("scale", 100)
        )
    }
}

class TriggerStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("dpfr_triggers", Context.MODE_PRIVATE)

    fun list(): List<TriggerRule> {
        val raw = prefs.getString("rules", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { TriggerRule.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun save(rule: TriggerRule) {
        val all = list().toMutableList()
        val index = all.indexOfFirst { it.id == rule.id }
        if (index >= 0) all[index] = rule else all.add(rule)
        write(all)
    }

    fun delete(id: String) {
        write(list().filter { it.id != id })
    }

    private fun write(rules: List<TriggerRule>) {
        val arr = JSONArray()
        rules.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("rules", arr.toString()).apply()
    }
}
