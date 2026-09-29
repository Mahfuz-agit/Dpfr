package com.dpfr.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colors taken from Apple's system palette, so the app feels like the HIG. */
object Ios {
    val Blue = Color(0xFF007AFF)
    val Green = Color(0xFF34C759)
    val Red = Color(0xFFFF3B30)
    val Orange = Color(0xFFFF9500)
}

private val LightColors = lightColorScheme(
    primary = Ios.Blue,
    onPrimary = Color.White,
    background = Color(0xFFF2F2F7),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6C6C70),
    outline = Color(0xFFC6C6C8),
    error = Ios.Red,
    secondaryContainer = Color(0xFFE3EFFF),
    onSecondaryContainer = Ios.Blue
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF1C1C1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF38383A),
    error = Color(0xFFFF453A),
    secondaryContainer = Color(0xFF1B3252),
    onSecondaryContainer = Color(0xFF0A84FF)
)

@Composable
fun DpfrTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(20.dp)
        ),
        content = content
    )
}

@Composable
fun LargeTitle(text: String, subtitle: String? = null) {
    Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = text,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp)
    )
}

/** Rounded grouped card, like an iOS inset grouped list. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(content = content)
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
fun IosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Ios.Green,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedBorderColor = Color.Transparent
        )
    )
}

/** Buttons are at least 44dp tall, as the HIG asks. */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val shape = RoundedCornerShape(12.dp)
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 44.dp),
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = tint, contentColor = Color.White)
        ) {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 44.dp),
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = tint.copy(alpha = 0.12f),
                contentColor = tint
            )
        ) {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}
