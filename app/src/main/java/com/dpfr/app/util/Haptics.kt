package com.dpfr.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.dpfr.app.data.Feature
import com.dpfr.app.data.FeatureSettings

object Haptics {

    fun tick(context: Context) {
        runCatching {
            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val effect = if (Build.VERSION.SDK_INT >= 29) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            } else {
                VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(effect)
        }
    }

    /** Vibrates only when the "Haptic feedback" switch is on. */
    fun tickIfEnabled(context: Context) {
        if (FeatureSettings(context).get(Feature.HAPTICS)) tick(context)
    }
}
