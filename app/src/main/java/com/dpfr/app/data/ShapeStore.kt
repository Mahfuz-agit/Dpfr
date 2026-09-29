package com.dpfr.app.data

import android.content.Context
import org.json.JSONArray

/** Saves the user's custom shapes as JSON in SharedPreferences. */
class ShapeStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("dpfr_shapes", Context.MODE_PRIVATE)

    fun list(): List<CustomShape> {
        val raw = prefs.getString("shapes", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { CustomShape.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun get(id: String): CustomShape? = list().firstOrNull { it.id == id }

    fun save(shape: CustomShape) {
        val all = list().toMutableList()
        val index = all.indexOfFirst { it.id == shape.id }
        if (index >= 0) all[index] = shape else all.add(shape)
        write(all)
    }

    fun delete(id: String) {
        write(list().filter { it.id != id })
    }

    private fun write(shapes: List<CustomShape>) {
        val arr = JSONArray()
        shapes.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("shapes", arr.toString()).apply()
    }
}
