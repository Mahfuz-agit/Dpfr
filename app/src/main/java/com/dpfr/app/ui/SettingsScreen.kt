package com.dpfr.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpfr.app.data.Feature
import com.dpfr.app.data.FeatureGroup
import com.dpfr.app.data.FeatureSettings
import com.dpfr.app.tile.DpfrTileService
import com.dpfr.app.trigger.AppTriggerService
import com.dpfr.app.trigger.UsageAccessHelper
import com.dpfr.app.util.AutoStartGuide
import com.dpfr.app.util.Haptics
import com.dpfr.app.util.PermissionHelper
import com.dpfr.app.util.ShortcutHelper
import com.dpfr.app.widget.WidgetRefresh

@Composable
fun SettingsScreen(tick: Int) {
    val ctx = LocalContext.current
    val settings = remember { FeatureSettings(ctx) }
    val states = remember {
        mutableStateMapOf<Feature, Boolean>().apply {
            Feature.entries.forEach { put(it, settings.get(it)) }
        }
    }
    var autoSeconds by remember { mutableFloatStateOf(settings.autoStopSeconds.toFloat()) }
    var dimIntensity by remember { mutableFloatStateOf(settings.dimmerIntensity) }
    var dimFeather by remember { mutableFloatStateOf(settings.dimmerFeatherDp) }

    val overlayOk = remember(tick) { PermissionHelper.canDrawOverlays(ctx) }
    val usageOk = remember(tick) { UsageAccessHelper.hasAccess(ctx) }

    fun onToggle(feature: Feature, value: Boolean) {
        states[feature] = value
        settings.set(feature, value)
        Haptics.tickIfEnabled(ctx)
        when (feature) {
            Feature.APP_TRIGGER -> {
                if (value && !usageOk) {
                    Toast.makeText(ctx, "Allow Usage access in the Triggers tab first", Toast.LENGTH_LONG).show()
                }
                AppTriggerService.sync(ctx)
            }
            Feature.WIDGETS -> WidgetRefresh.all(ctx)
            Feature.SHORTCUTS -> ShortcutHelper.publish(ctx)
            Feature.TILE -> DpfrTileService.refresh(ctx)
            else -> Unit
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { LargeTitle("Settings", "Every feature has its own switch.") }

        FeatureGroup.entries.forEach { group ->
            item { SectionLabel(group.title) }
            item {
                val features = Feature.entries.filter { it.group == group }
                GroupCard(Modifier.fillMaxWidth()) {
                    features.forEachIndexed { index, feature ->
                        if (index > 0) RowDivider()
                        FeatureRow(
                            title = feature.title,
                            subtitle = feature.subtitle,
                            checked = states[feature] == true,
                            onChange = { onToggle(feature, it) }
                        )

                        if (feature == Feature.AUTO_STOP && states[feature] == true) {
                            SliderRow(
                                label = "Stop after ${autoSeconds.toInt()} seconds",
                                value = autoSeconds,
                                range = 5f..600f,
                                onChange = { autoSeconds = it },
                                onFinished = { settings.autoStopSeconds = autoSeconds.toInt() }
                            )
                        }
                        if (feature == Feature.DIMMER && states[feature] == true) {
                            SliderRow(
                                label = "Darkness ${(dimIntensity * 100).toInt()}%",
                                value = dimIntensity,
                                range = 0.1f..1f,
                                onChange = { dimIntensity = it },
                                onFinished = { settings.dimmerIntensity = dimIntensity }
                            )
                            SliderRow(
                                label = "Soft edge ${dimFeather.toInt()} dp",
                                value = dimFeather,
                                range = 0f..60f,
                                onChange = { dimFeather = it },
                                onFinished = { settings.dimmerFeatherDp = dimFeather }
                            )
                        }
                    }
                }
            }
        }

        item { SectionLabel("Reliability") }
        item {
            GroupCard(Modifier.fillMaxWidth()) {
                StatusRow("Display over other apps", overlayOk)
                RowDivider()
                StatusRow("Usage access (app trigger)", usageOk)
                RowDivider()
                Column(Modifier.padding(16.dp)) {
                    Text("Battery and auto-start", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Some phones stop background apps. Allow auto-start and remove " +
                            "battery limits for Dpfr so pranks and triggers keep working.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    ActionButton(
                        text = "Open phone settings",
                        onClick = {
                            if (!AutoStartGuide.open(ctx)) {
                                Toast.makeText(ctx, "No matching settings screen found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        filled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item { SectionLabel("Good to know") }
        item {
            GroupCard(Modifier.fillMaxWidth()) {
                Text(
                    text = "The dimmer is a black layer, not real brightness. Battery is not saved. " +
                        "Overlays are kept slightly see-through so touches still reach the apps below. " +
                        "Use pranks on your own phone or with the owner's consent.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IosSwitch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onFinished,
            valueRange = range
        )
    }
}

@Composable
private fun StatusRow(title: String, ok: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text(
            text = if (ok) "Allowed" else "Not allowed",
            fontSize = 15.sp,
            color = if (ok) Ios.Green else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
