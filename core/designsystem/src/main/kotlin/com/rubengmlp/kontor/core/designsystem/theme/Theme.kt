package com.rubengmlp.kontor.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightScheme = lightColorScheme(
    primary = Evergreen,
    onPrimary = Color.White,
    primaryContainer = EvergreenSoft,
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF4F635A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8DC),
    onSecondaryContainer = Color(0xFF0B1F17),
    tertiary = Color(0xFF3D6373),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC1E8FA),
    onTertiaryContainer = Color(0xFF001F2A),
    background = Paper,
    onBackground = Color(0xFF111814),
    surface = Cloud,
    onSurface = Color(0xFF111814),
    surfaceVariant = Mist,
    onSurfaceVariant = Color(0xFF3F4944),
    outline = Graphite,
    outlineVariant = Color(0xFFC3CBC5),
    error = Coral,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Ink,
    inverseOnSurface = Color(0xFFEDF2ED),
    inversePrimary = Mint,
)

private val DarkScheme = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF003828),
    primaryContainer = MintContainer,
    onPrimaryContainer = Color(0xFFAAF2D9),
    secondary = Color(0xFFB6CCBF),
    onSecondary = Color(0xFF22352C),
    secondaryContainer = Color(0xFF384C42),
    onSecondaryContainer = Color(0xFFD2E8DC),
    tertiary = Color(0xFFA5CCDF),
    onTertiary = Color(0xFF073543),
    tertiaryContainer = Color(0xFF254B5A),
    onTertiaryContainer = Color(0xFFC1E8FA),
    background = DarkSurface,
    onBackground = Color(0xFFDDE5DF),
    surface = DarkSurface,
    onSurface = Color(0xFFDDE5DF),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = DarkOutline,
    outlineVariant = Color(0xFF3F4944),
    error = DarkCoral,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFDDE5DF),
    inverseOnSurface = Color(0xFF1A211D),
    inversePrimary = Evergreen,
)

@Immutable
data class KontorExtraColors(
    val positive: Color,
    val negative: Color,
    val pending: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val heroMuted: Color,
    val accent: Color,
    val cardSage: Color,
    val onCardSage: Color,
    val cardInk: Color,
    val onCardInk: Color,
)

private val LightExtras = KontorExtraColors(
    positive = Color(0xFF1B7A55),
    negative = Color(0xFFB3261E),
    pending = Amber,
    heroStart = Evergreen,
    heroEnd = EvergreenDeep,
    onHero = Color.White,
    heroMuted = Color(0xFFCDEBDF),
    accent = Lime,
    cardSage = Color(0xFFDDF2E5),
    onCardSage = Color(0xFF113B2C),
    cardInk = Color(0xFF1B2A24),
    onCardInk = Color(0xFFEAF4EE),
)

private val DarkExtras = LightExtras.copy(
    positive = Mint,
    negative = DarkCoral,
    pending = DarkAmber,
    heroStart = Color(0xFF0E4A3C),
    heroEnd = Color(0xFF082C24),
    heroMuted = Color(0xFFB9D8CB),
    cardSage = Color(0xFF174435),
    onCardSage = Color(0xFFD7F3E5),
    cardInk = Color(0xFF20312A),
    onCardInk = Color(0xFFE6F0EA),
)

val LocalKontorExtraColors = staticCompositionLocalOf { LightExtras }

object KontorTheme {
    val colors: KontorExtraColors
        @Composable get() = LocalKontorExtraColors.current
}

private val KontorShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp),
)

@Composable
fun KontorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalKontorExtraColors provides if (darkTheme) DarkExtras else LightExtras,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = KontorTypography,
            shapes = KontorShapes,
            content = content,
        )
    }
}
