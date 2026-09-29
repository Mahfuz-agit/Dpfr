package com.dpfr.app.ui

import android.app.Activity
import android.graphics.PointF
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dpfr.app.data.CustomShape
import com.dpfr.app.data.ShapeKind
import com.dpfr.app.data.ShapeStore
import com.dpfr.app.data.FeatureSettings
import com.dpfr.app.overlay.PrankLauncher
import com.dpfr.app.overlay.views.CustomShapeView
import com.dpfr.app.util.Haptics
import com.dpfr.app.util.ShortcutHelper
import com.dpfr.app.widget.WidgetRefresh
import java.util.UUID
import kotlin.math.max

private val palette: List<Int> = listOf(
    0xFF000000, 0xFFFFFFFF, 0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00, 0xFF34C759,
    0xFF00C7BE, 0xFF007AFF, 0xFF5856D6, 0xFFAF52DE, 0xFFFF2D55, 0xFF8E8E93
).map { it.toInt() }

@Composable
fun ShapeEditorScreen() {
    val ctx = LocalContext.current
    val store = remember { ShapeStore(ctx) }

    var saved by remember { mutableStateOf(store.list()) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(ShapeKind.CIRCLE) }
    var raw by remember { mutableStateOf(listOf<Offset>()) }
    var ratio by remember { mutableFloatStateOf(1f) }
    var fill by remember { mutableIntStateOf(0xFF000000.toInt()) }
    var opacity by remember { mutableFloatStateOf(85f) }
    var blur by remember { mutableFloatStateOf(0f) }
    var borderColor by remember { mutableIntStateOf(0xFFFFFFFF.toInt()) }
    var borderWidth by remember { mutableFloatStateOf(0f) }

    val draft = remember(kind, raw, ratio, fill, opacity, blur, borderColor, borderWidth) {
        buildShape(
            id = editingId ?: "draft",
            name = name,
            kind = kind,
            raw = raw,
            ratio = ratio,
            fill = fill,
            opacityPct = opacity.toInt(),
            blurDp = blur,
            borderColor = borderColor,
            borderWidthDp = borderWidth
        )
    }

    val canSave = name.isNotBlank() && (kind != ShapeKind.FREEHAND || raw.size >= 8)

    fun resetForm() {
        editingId = null
        name = ""
        raw = emptyList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LargeTitle(
                if (editingId == null) "Shapes" else "Edit shape",
                "Make your own shape and put it anywhere."
            )
        }

        item {
            GroupCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewWidth = (160f * draft.aspect).coerceIn(60f, 280f)
                        AndroidView(
                            factory = { CustomShapeView(it) },
                            update = { it.shape = draft },
                            modifier = Modifier
                                .width(previewWidth.dp)
                                .height(160.dp)
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ShapeKind.entries.toList()) { k ->
                            FilterChip(
                                selected = kind == k,
                                onClick = { kind = k },
                                label = { Text(k.label) }
                            )
                        }
                    }

                    if (kind == ShapeKind.FREEHAND) {
                        FreehandPad(
                            points = raw,
                            onPoint = { p ->
                                val last = raw.lastOrNull()
                                if (last == null || (p - last).getDistance() > 8f) raw = raw + p
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (raw.size < 8) "Draw a closed outline with your finger." else "Looks good.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton("Clear", { raw = emptyList() }, filled = false)
                        }
                    } else {
                        LabeledSlider("Wide or tall  ${"%.2f".format(ratio)}", ratio, 0.3f..3f) { ratio = it }
                    }

                    Text("Fill color", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ColorRow(fill) { fill = it }
                    LabeledSlider("Opacity  ${opacity.toInt()}%", opacity, 5f..100f) { opacity = it }
                    LabeledSlider("Blur  ${blur.toInt()} dp", blur, 0f..24f) { blur = it }
                    LabeledSlider("Border  ${"%.1f".format(borderWidth)} dp", borderWidth, 0f..12f) { borderWidth = it }
                    if (borderWidth > 0f) {
                        Text("Border color", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ColorRow(borderColor) { borderColor = it }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(24) },
                        label = { Text("Shape name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (editingId != null) {
                            ActionButton(
                                "Cancel", { resetForm() },
                                filled = false, modifier = Modifier.weight(1f)
                            )
                        }
                        ActionButton(
                            text = "Save shape",
                            enabled = canSave,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                Haptics.tickIfEnabled(ctx)
                                val shape = draft.copy(id = editingId ?: UUID.randomUUID().toString())
                                store.save(shape)
                                saved = store.list()
                                WidgetRefresh.all(ctx)
                                ShortcutHelper.publish(ctx)
                                Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show()
                                resetForm()
                            }
                        )
                    }
                }
            }
        }

        item { SectionLabel("Saved shapes") }

        if (saved.isEmpty()) {
            item {
                Text(
                    text = "No saved shapes yet.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        items(saved, key = { it.id }) { shape ->
            GroupCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { CustomShapeView(it) },
                                update = { it.shape = shape },
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(shape.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                shape.kind.label,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = "Place",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (PrankLauncher.launch(ctx, "CUSTOM:${shape.id}", edit = true)) {
                                    Toast.makeText(
                                        ctx,
                                        "Drag to move. Pinch to resize. Tap Done when finished.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    (ctx as? Activity)?.moveTaskToBack(true)
                                }
                            }
                        )
                        ActionButton(
                            text = "Edit",
                            filled = false,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                editingId = shape.id
                                name = shape.name
                                kind = shape.kind
                                ratio = shape.aspect.coerceIn(0.3f, 3f)
                                fill = shape.fillColor
                                opacity = shape.opacityPct.toFloat()
                                blur = shape.blurDp
                                borderColor = shape.borderColor
                                borderWidth = shape.borderWidthDp
                                raw = if (shape.kind == ShapeKind.FREEHAND) {
                                    val h = 200f
                                    val w = 200f * shape.aspect
                                    shape.points.map { Offset(it.x * w, it.y * h) }
                                } else {
                                    emptyList()
                                }
                            }
                        )
                        ActionButton(
                            text = "Delete",
                            filled = false,
                            tint = Ios.Red,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                store.delete(shape.id)
                                val settings = FeatureSettings(ctx)
                                settings.favorites = settings.favorites.filter { it != "CUSTOM:${shape.id}" }
                                saved = store.list()
                                if (editingId == shape.id) resetForm()
                                WidgetRefresh.all(ctx)
                                ShortcutHelper.publish(ctx)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FreehandPad(points: List<Offset>, onPoint: (Offset) -> Unit) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onPoint(it) },
                    onDrag = { change, _ ->
                        change.consume()
                        onPoint(change.position)
                    }
                )
            }
    ) {
        if (points.size >= 2) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ColorRow(selected: Int, onPick: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(palette) { c ->
            val isSelected = c == selected
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onPick(c) }
            )
        }
    }
}

private fun buildShape(
    id: String,
    name: String,
    kind: ShapeKind,
    raw: List<Offset>,
    ratio: Float,
    fill: Int,
    opacityPct: Int,
    blurDp: Float,
    borderColor: Int,
    borderWidthDp: Float
): CustomShape {
    var points = emptyList<PointF>()
    var aspect = ratio
    if (kind == ShapeKind.FREEHAND) {
        aspect = 1f
        if (raw.size >= 3) {
            val minX = raw.minOf { it.x }
            val maxX = raw.maxOf { it.x }
            val minY = raw.minOf { it.y }
            val maxY = raw.maxOf { it.y }
            val w = max(maxX - minX, 1f)
            val h = max(maxY - minY, 1f)
            points = raw.map { PointF((it.x - minX) / w, (it.y - minY) / h) }
            aspect = (w / h).coerceIn(0.2f, 5f)
        }
    }
    return CustomShape(
        id = id,
        name = name,
        kind = kind,
        points = points,
        aspect = aspect,
        fillColor = fill,
        opacityPct = opacityPct,
        blurDp = blurDp,
        borderColor = borderColor,
        borderWidthDp = borderWidthDp
    )
}
