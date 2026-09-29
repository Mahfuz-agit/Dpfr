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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.dpfr.app.data.FeatureSettings
import com.dpfr.app.data.ShapeStore
import com.dpfr.app.overlay.PrankRef

private data class Slot(val key: String, val label: String)

/** 4x2 widget: four favorite pranks and a Stop button. Favorites are picked with the star in the app. */
class MediumWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = FeatureSettings(context)
        val shapes = ShapeStore(context).list()
        val slots = settings.favorites
            .mapNotNull { key ->
                PrankRef.parse(key)?.let { Slot(key, PrankLabels.short(shapes, it)) }
            }
            .take(4)

        provideContent {
            GlanceTheme {
                MediumContent(context, slots)
            }
        }
    }
}

@Composable
private fun MediumContent(context: Context, slots: List<Slot>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(12.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dpfr",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
            Button(
                text = "Stop all",
                onClick = actionStartActivity(QuickIntents.stop(context, "widget")),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = GlanceTheme.colors.errorContainer,
                    contentColor = GlanceTheme.colors.onErrorContainer
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(10.dp))
        if (slots.isEmpty()) {
            Text(
                text = "Star pranks in the app to show them here.",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                slots.forEachIndexed { index, slot ->
                    if (index > 0) Spacer(modifier = GlanceModifier.width(6.dp))
                    Button(
                        text = slot.label,
                        onClick = actionStartActivity(QuickIntents.start(context, slot.key, "widget")),
                        modifier = GlanceModifier.defaultWeight(),
                        maxLines = 1,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = GlanceTheme.colors.primary,
                            contentColor = GlanceTheme.colors.onPrimary
                        )
                    )
                }
            }
        }
    }
}

class MediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumWidget()
}
