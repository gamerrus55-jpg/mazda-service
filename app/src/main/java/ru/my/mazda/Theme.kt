package ru.my.mazda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Палитра: красная, как машина ----------

val Red = Color(0xFFD71920)
val RedDark = Color(0xFF8E0E15)
val RedDeep = Color(0xFF5A0A0F)
val Amber = Color(0xFFF59E0B)
val Green = Color(0xFF16A34A)
val Gray = Color(0xFF8A909C)

val HeroBrush: Brush get() = Brush.linearGradient(listOf(Color(0xFFE5262E), RedDark, RedDeep))
val RedBrush: Brush get() = Brush.linearGradient(listOf(Color(0xFFE5262E), RedDark))

private val LightColors = lightColorScheme(
    primary = Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1E2),
    onPrimaryContainer = RedDeep,
    secondaryContainer = Color(0xFFFCEDEE),
    onSecondaryContainer = RedDeep,
    background = Color(0xFFF7F3F3),
    onBackground = Color(0xFF1C1416),
    surface = Color.White,
    onSurface = Color(0xFF1C1416),
    surfaceVariant = Color(0xFFF1E8E9),
    onSurfaceVariant = Color(0xFF6E5E61),
    outlineVariant = Color(0xFFEADDDF),
    error = Red,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF5A5F),
    onPrimary = Color(0xFF3A0508),
    primaryContainer = Color(0xFF5A0A0F),
    onPrimaryContainer = Color(0xFFFFDADB),
    secondaryContainer = Color(0xFF2E1A1C),
    onSecondaryContainer = Color(0xFFFFDADB),
    background = Color(0xFF120C0D),
    onBackground = Color(0xFFF6ECED),
    surface = Color(0xFF1E1517),
    onSurface = Color(0xFFF6ECED),
    surfaceVariant = Color(0xFF2B2022),
    onSurfaceVariant = Color(0xFFC3B1B3),
    outlineVariant = Color(0xFF3A2C2E),
    error = Color(0xFFFF5A5F),
)

private val AppTypography: Typography
    get() {
        val t = Typography()
        return t.copy(
            displaySmall = t.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
            headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
            headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
            titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 19.sp),
            titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}

/** Подложка под иллюстрацию узла: светло-розовая в светлой теме. */
val PicBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF3A2427) else Color(0xFFFCEDEE)

// ---------- Компоненты ----------

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    radius: Dp = 22.dp,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val base = modifier
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    Column(
        (if (onClick != null) base.clickable(onClick = onClick) else base).padding(padding),
        content = content
    )
}

@Composable
fun RedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .height(54.dp)
            .clip(shape)
            .background(if (enabled) RedBrush else Brush.linearGradient(listOf(Color(0xFFC4B8BA), Color(0xFFC4B8BA))))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
    }
}

@Composable
fun Pill(text: String, onClick: () -> Unit, selected: Boolean = false, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    val mod = modifier
        .clip(shape)
        .then(
            if (selected) Modifier.background(RedBrush)
            else Modifier
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 10.dp)
    Box(mod, contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun Bar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 7.dp) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0.03f, 1f))
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

@Composable
fun CircleButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** Цветная метка статуса. */
@Composable
fun Tag(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
