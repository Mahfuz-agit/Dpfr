package com.dpfr.app.overlay

import android.content.Context
import android.provider.Settings
import android.widget.Toast
import com.dpfr.app.data.FeatureSettings
import com.dpfr.app.util.PermissionHelper
import com.dpfr.app.widget.WidgetRefresh

/** One place that checks switches and permissions before a prank starts. */
object PrankLauncher {

    fun launch(context: Context, key: String, edit: Boolean, interactive: Boolean = true): Boolean {
        val settings = FeatureSettings(context)
        val ref = PrankRef.parse(key) ?: return false

        if (!settings.isPrankEnabled(ref)) {
            toast(context, "${ref.type.feature.title} is turned off in Settings")
            return false
        }
        if (!Settings.canDrawOverlays(context)) {
            toast(context, "Allow \"Display over other apps\" first")
            if (interactive) {
                runCatching { context.startActivity(PermissionHelper.overlaySettingsIntent(context)) }
            }
            return false
        }
        return try {
            OverlayService.add(context, key, edit)
            settings.lastPrank = key
            WidgetRefresh.all(context)
            true
        } catch (e: Exception) {
            toast(context, "Could not start: ${e.javaClass.simpleName}")
            false
        }
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
