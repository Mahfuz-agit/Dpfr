package com.dpfr.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.dpfr.app.data.FeatureSettings
import com.dpfr.app.overlay.PrankRef
import com.dpfr.app.overlay.PrankType

/** 2x1 widget: shows the last prank with Start and Stop buttons. */
class SmallWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = FeatureSettings(context)
        val key = settings.lastPrank ?: PrankType.CRACKED_1.name
        val label = PrankRef.parse(key)?.let { PrankLabels.label(context, it) }
            ?: PrankType.CRACKED_1.label

        provideContent {
            GlanceTheme {
                SmallContent(context, key, label)
            }
        }
    }
}

@Composable
private fun SmallContent(context: Context, key: String, label: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "Dpfr",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            Text(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }
        Button(
            text = "Start",
            onClick = actionStartActivity(QuickIntents.start(context, key, "widget")),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = GlanceTheme.colors.primary,
                contentColor = GlanceTheme.colors.onPrimary
            )
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Button(
            text = "Stop",
            onClick = actionStartActivity(QuickIntents.stop(context, "widget")),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = GlanceTheme.colors.errorContainer,
                contentColor = GlanceTheme.colors.onErrorContainer
            )
        )
    }
}

class SmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallWidget()
}
