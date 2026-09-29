package com.dpfr.app.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpfr.app.data.FeatureSettings
import com.dpfr.app.data.ShapeStore
import com.dpfr.app.overlay.OverlayService
import com.dpfr.app.overlay.PrankLauncher
import com.dpfr.app.overlay.PrankOption
import com.dpfr.app.overlay.PrankOptions
import com.dpfr.app.util.Haptics
import com.dpfr.app.util.PermissionHelper
import com.dpfr.app.util.ShortcutHelper
import com.dpfr.app.widget.WidgetRefresh
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(tick: Int) {
    val ctx = LocalContext.current
    val settings = remember { FeatureSettings(ctx) }

    var running by remember { mutableStateOf(OverlayService.running) }
    LaunchedEffect(Unit) {
        while (true) {
            running = OverlayService.running
            delay(700)
        }
    }

    val overlayOk = remember(tick) { PermissionHelper.canDrawOverlays(ctx) }
    var notifOk by remember(tick) { mutableStateOf(PermissionHelper.notificationsGranted(ctx)) }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notifOk = it }

    var favorites by remember { mutableStateOf(settings.favorites) }
    val options = remember(tick) { PrankOptions.all(ShapeStore(ctx).list()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { LargeTitle("Dpfr", "Display prank toolkit") }

        if (!overlayOk || !notifOk) {
            item {
                GroupCard(Modifier.fillMaxWidth()) {
                    SetupRow(
                        title = "Display over other apps",
                        done = overlayOk,
                        onGrant = {
                            runCatching { ctx.startActivity(PermissionHelper.overlaySettingsIntent(ctx)) }
                        }
                    )
                    RowDivider()
                    SetupRow(
                        title = "Notifications (for the Stop button)",
                        done = notifOk,
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= 33) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }
        }

        item {
            ActionButton(
                text = if (running) "Stop all pranks" else "No active pranks",
                onClick = {
                    Haptics.tickIfEnabled(ctx)
                    OverlayService.stopAll(ctx)
                },
                enabled = running,
                tint = Ios.Red,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { SectionLabel("Pranks") }

        items(options, key = { it.key }) { option ->
            val isFavorite = option.key in favorites
            PrankRow(
                option = option,
                enabled = settings.get(option.feature),
                isFavorite = isFavorite,
                onToggleFavorite = {
                    val updated = when {
                        isFavorite -> favorites - option.key
                        favorites.size >= 4 -> {
                            Toast.makeText(ctx, "Only 4 favorites. Remove one first.", Toast.LENGTH_SHORT).show()
                            favorites
                        }
                        else -> favorites + option.key
                    }
                    favorites = updated
                    settings.favorites = updated
                    WidgetRefresh.all(ctx)
                    ShortcutHelper.publish(ctx)
                },
                onPlace = {
                    Haptics.tickIfEnabled(ctx)
                    if (PrankLauncher.launch(ctx, option.key, edit = true)) {
                        Toast.makeText(
                            ctx,
                            "Drag to move. Pinch to resize. Tap Done when finished.",
                            Toast.LENGTH_LONG
                        ).show()
                        (ctx as? Activity)?.moveTaskToBack(true)
                    }
                },
                onQuick = {
                    Haptics.tickIfEnabled(ctx)
                    PrankLauncher.launch(ctx, option.key, edit = false)
                }
            )
        }
    }
}

@Composable
private fun SetupRow(title: String, done: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) Ios.Green else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            fontSize = 16.sp
        )
        if (!done) ActionButton("Allow", onGrant, filled = false)
    }
}

@Composable
private fun PrankRow(
    option: PrankOption,
    enabled: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPlace: () -> Unit,
    onQuick: () -> Unit
) {
    GroupCard(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(option.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (enabled) option.hint else "Turned off in Settings",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Ios.Orange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    text = "Place",
                    onClick = onPlace,
                    filled = false,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = "Quick start",
                    onClick = onQuick,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
