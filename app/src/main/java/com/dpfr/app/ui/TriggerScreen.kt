package com.dpfr.app.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.dpfr.app.data.Feature
import com.dpfr.app.data.FeatureSettings
import com.dpfr.app.data.ShapeStore
import com.dpfr.app.data.TriggerRule
import com.dpfr.app.data.TriggerStore
import com.dpfr.app.overlay.PrankOption
import com.dpfr.app.overlay.PrankOptions
import com.dpfr.app.trigger.AppTriggerService
import com.dpfr.app.trigger.UsageAccessHelper
import com.dpfr.app.util.Haptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

private data class AppInfo(val label: String, val pkg: String)

@Composable
fun TriggerScreen(tick: Int) {
    val ctx = LocalContext.current
    val settings = remember { FeatureSettings(ctx) }
    val store = remember { TriggerStore(ctx) }
    val options = remember { PrankOptions.all(ShapeStore(ctx).list()) }

    var rules by remember { mutableStateOf(store.list()) }
    var editing by remember { mutableStateOf<TriggerRule?>(null) }
    var triggerOn by remember { mutableStateOf(settings.get(Feature.APP_TRIGGER)) }
    val usageOk = remember(tick) { UsageAccessHelper.hasAccess(ctx) }

    val current = editing
    if (current != null) {
        RuleEditor(
            initial = current,
            options = options,
            onCancel = { editing = null },
            onSave = { rule ->
                store.save(rule)
                rules = store.list()
                editing = null
                AppTriggerService.sync(ctx)
            }
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LargeTitle("Triggers", "A prank starts when a chosen app opens and ends when you leave it.")
        }

        item {
            GroupCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("App trigger", fontSize = 17.sp)
                        Text(
                            "Master switch for all rules",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IosSwitch(
                        checked = triggerOn,
                        onCheckedChange = {
                            triggerOn = it
                            settings.set(Feature.APP_TRIGGER, it)
                            Haptics.tickIfEnabled(ctx)
                            AppTriggerService.sync(ctx)
                        }
                    )
                }
                RowDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Usage access", fontSize = 17.sp)
                        Text(
                            text = if (usageOk) "Allowed" else "Needed to see which app is open",
                            fontSize = 13.sp,
                            color = if (usageOk) Ios.Green else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!usageOk) {
                        ActionButton("Allow", { UsageAccessHelper.openSettings(ctx) }, filled = false)
                    }
                }
            }
        }

        item {
            ActionButton(
                text = "Add rule",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    editing = TriggerRule(
                        id = UUID.randomUUID().toString(),
                        packageName = "",
                        appLabel = "",
                        prankKey = options.first().key,
                        enabled = true,
                        xPct = 50,
                        yPct = 50,
                        scalePct = 100
                    )
                }
            )
        }

        item { SectionLabel("Rules") }

        if (rules.isEmpty()) {
            item {
                Text(
                    "No rules yet.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        items(rules, key = { it.id }) { rule ->
            GroupCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(rule.packageName, 44.dp)
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                rule.appLabel.ifBlank { rule.packageName },
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val prankName = options.firstOrNull { it.key == rule.prankKey }?.label ?: "Missing prank"
                            Text(
                                prankName,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IosSwitch(
                            checked = rule.enabled,
                            onCheckedChange = {
                                store.save(rule.copy(enabled = it))
                                rules = store.list()
                                Haptics.tickIfEnabled(ctx)
                                AppTriggerService.sync(ctx)
                            }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            "Edit", { editing = rule },
                            filled = false, modifier = Modifier.weight(1f)
                        )
                        ActionButton(
                            "Delete",
                            {
                                store.delete(rule.id)
                                rules = store.list()
                                AppTriggerService.sync(ctx)
                            },
                            filled = false, tint = Ios.Red, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleEditor(
    initial: TriggerRule,
    options: List<PrankOption>,
    onCancel: () -> Unit,
    onSave: (TriggerRule) -> Unit
) {
    val ctx = LocalContext.current
    var draft by remember { mutableStateOf(initial) }
    var picker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { LargeTitle("Trigger rule") }

        item {
            GroupCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { picker = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (draft.packageName.isNotBlank()) {
                        AppIcon(draft.packageName, 44.dp)
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = if (draft.packageName.isNotBlank()) 12.dp else 0.dp)
                    ) {
                        Text("App", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = draft.appLabel.ifBlank { draft.packageName.ifBlank { "Tap to choose an app" } },
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (draft.packageName.isNotBlank()) {
                            Text(
                                draft.packageName,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item { SectionLabel("Prank") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options, key = { it.key }) { option ->
                    FilterChip(
                        selected = draft.prankKey == option.key,
                        onClick = { draft = draft.copy(prankKey = option.key) },
                        label = { Text(option.label) }
                    )
                }
            }
        }

        item { SectionLabel("Position") }
        item {
            GroupCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PhonePreview(draft.xPct, draft.yPct, draft.scalePct)
                    Spacer(Modifier.height(8.dp))
                    PercentSlider("Left to right  ${draft.xPct}%", draft.xPct, 0..100) {
                        draft = draft.copy(xPct = it)
                    }
                    PercentSlider("Top to bottom  ${draft.yPct}%", draft.yPct, 0..100) {
                        draft = draft.copy(yPct = it)
                    }
                    PercentSlider("Size  ${draft.scalePct}%", draft.scalePct, 20..150) {
                        draft = draft.copy(scalePct = it)
                    }
                    Text(
                        "The preview is only a guide. Big pranks like cracks fill the screen at 100%.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Cancel", onCancel, filled = false, modifier = Modifier.weight(1f))
                ActionButton(
                    text = "Save rule",
                    enabled = draft.packageName.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        Haptics.tickIfEnabled(ctx)
                        onSave(draft)
                    }
                )
            }
        }
    }

    if (picker) {
        AppPickerDialog(
            onDismiss = { picker = false },
            onPick = { label, pkg ->
                draft = draft.copy(appLabel = label, packageName = pkg)
                picker = false
            }
        )
    }
}

@Composable
private fun PercentSlider(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}

@Composable
private fun PhonePreview(xPct: Int, yPct: Int, scalePct: Int) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BoxWithConstraints(
            modifier = Modifier
                .height(220.dp)
                .aspectRatio(9f / 19.5f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
        ) {
            val boxW = maxWidth * 0.45f * (scalePct / 100f)
            val boxH = maxHeight * 0.20f * (scalePct / 100f)
            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * (xPct / 100f) - boxW / 2,
                        y = maxHeight * (yPct / 100f) - boxH / 2
                    )
                    .size(boxW, boxH)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
            )
        }
    }
}

@Composable
private fun AppIcon(pkg: String, size: androidx.compose.ui.unit.Dp) {
    val ctx = LocalContext.current
    val bitmap = remember(pkg) {
        runCatching { ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }
            .getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Box(
            Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@Composable
private fun AppPickerDialog(onDismiss: () -> Unit, onPick: (label: String, pkg: String) -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInfo>?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.Default) { loadLauncherApps(ctx) }
    }

    val filtered = apps.orEmpty().filter {
        query.isBlank() ||
            it.label.contains(query, ignoreCase = true) ||
            it.pkg.contains(query, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Choose an app", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search, or type a package name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                val typed = query.trim()
                val looksLikePackage = typed.contains('.') && !typed.contains(' ')
                if (looksLikePackage && filtered.none { it.pkg == typed }) {
                    ActionButton(
                        text = "Use $typed",
                        onClick = { onPick(typed, typed) },
                        filled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (apps == null) {
                    Text("Loading apps...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(filtered, key = { it.pkg }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(app.label, app.pkg) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(app.pkg, 40.dp)
                                Column(Modifier.padding(start = 12.dp)) {
                                    Text(app.label, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        app.pkg,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                ActionButton("Close", onDismiss, filled = false, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Uses the launcher-intent query declared in the manifest, so no broad package permission is needed. */
private fun loadLauncherApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = if (android.os.Build.VERSION.SDK_INT >= 33) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
    }
    return resolved
        .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}
