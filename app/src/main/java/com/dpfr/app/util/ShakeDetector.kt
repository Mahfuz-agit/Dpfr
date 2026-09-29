package com.dpfr.app.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

/** Calls [onShake] after three strong shakes inside about one second. */
class ShakeDetector(context: Context, private val onShake: () -> Unit) : SensorEventListener {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var count = 0
    private var lastShakeTime = 0L
    private var firstShakeTime = 0L

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val gx = event.values[0] / SensorManager.GRAVITY_EARTH
        val gy = event.values[1] / SensorManager.GRAVITY_EARTH
        val gz = event.values[2] / SensorManager.GRAVITY_EARTH
        val g = sqrt(gx * gx + gy * gy + gz * gz)
        if (g < 2.6f) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastShakeTime < 180L) return
        lastShakeTime = now

        if (now - firstShakeTime > 1200L) {
            count = 0
            firstShakeTime = now
        }
        count++
        if (count >= 3) {
            count = 0
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
