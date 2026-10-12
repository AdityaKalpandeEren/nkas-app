package com.kalpande.algopulse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Semantic trading colours that Material's scheme doesn't have. */
@Immutable
data class TradeColors(
    val profit: Color,
    val loss: Color,
    val neutral: Color,
    val profitContainer: Color,
    val lossContainer: Color,
    val warn: Color,
    val warnContainer: Color,
    val live: Color,
    val down: Color,
    val grid: Color,
    val accent2: Color,
    val heroGradient: List<Color>,
)

private val DarkTrade = TradeColors(
    profit = Color(0xFF34D399),
    loss = Color(0xFFFB7185),
    neutral = Color(0xFF94A3B8),
    profitContainer = Color(0xFF0F2E25),
    lossContainer = Color(0xFF3A1720),
    warn = Color(0xFFFBBF24),
    warnContainer = Color(0xFF3A2E10),
    live = Color(0xFF34D399),
    down = Color(0xFFF43F5E),
    grid = Color(0xFF243042),
    accent2 = Color(0xFF818CF8),
    heroGradient = listOf(Color(0xFF13202F), Color(0xFF0E2A2A)),
)

private val LightTrade = TradeColors(
    profit = Color(0xFF059669),
    loss = Color(0xFFE11D48),
    neutral = Color(0xFF64748B),
    profitContainer = Color(0xFFDDF6EC),
    lossContainer = Color(0xFFFDE2E7),
    warn = Color(0xFFB45309),
    warnContainer = Color(0xFFFEF3C7),
    live = Color(0xFF059669),
    down = Color(0xFFE11D48),
    grid = Color(0xFFE2E8F0),
    accent2 = Color(0xFF4F46E5),
    heroGradient = listOf(Color(0xFFFFFFFF), Color(0xFFEFFAF6)),
)

val LocalTradeColors = staticCompositionLocalOf { DarkTrade }

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF032420),
    primaryContainer = Color(0xFF0E3B37),
    onPrimaryContainer = Color(0xFF99F6E4),
    secondary = Color(0xFF818CF8),
    onSecondary = Color(0xFF111436),
    secondaryContainer = Color(0xFF232A55),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF2B1D00),
    background = Color(0xFF0A0E14),
    onBackground = Color(0xFFE6EDF6),
    surface = Color(0xFF0A0E14),
    onSurface = Color(0xFFE6EDF6),
    surfaceVariant = Color(0xFF1B2431),
    onSurfaceVariant = Color(0xFF93A1B5),
    surfaceContainerLowest = Color(0xFF070A0F),
    surfaceContainerLow = Color(0xFF0F151E),
    surfaceContainer = Color(0xFF131A25),
    surfaceContainerHigh = Color(0xFF19212E),
    surfaceContainerHighest = Color(0xFF212B3A),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF222C3A),
    error = Color(0xFFFB7185),
    onError = Color(0xFF3B0A14),
    inverseSurface = Color(0xFFE6EDF6),
    inverseOnSurface = Color(0xFF0F172A),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF053B36),
    secondary = Color(0xFF4F46E5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF1E1B4B),
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF6F8FB),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = Color(0xFF5B6B80),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBFCFE),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1F4F9),
    surfaceContainerHighest = Color(0xFFE8EDF4),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE3E9F1),
    error = Color(0xFFE11D48),
    onError = Color(0xFFFFFFFF),
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFF1F5F9),
)

private val base = Typography()

/** Roboto with tabular figures on everything numeric-heavy (columns of P&L line up). */
private val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp, fontSize = 34.sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Tabular (monospaced) digits for numbers. */
fun TextStyle.tnum(): TextStyle = copy(fontFeatureSettings = "tnum")

val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 0.sp)

@Composable
fun AlgoPulseTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTradeColors provides if (dark) DarkTrade else LightTrade) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object AppTheme {
    val trade: TradeColors
        @Composable get() = LocalTradeColors.current
}
