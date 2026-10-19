package io.github.halilozel1903.openf1.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val LightColors = lightColorScheme(
    primary = Color(0xFFD70A12),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFF5B5F6B),
    onSecondary = Color.White,
    tertiary = Color(0xFF8E24AA),
    background = Color(0xFFF6F6F8),
    onBackground = Color(0xFF15151E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF15151E),
    surfaceVariant = Color(0xFFE9E9EE),
    onSurfaceVariant = Color(0xFF55576A),
    surfaceContainer = Color(0xFFEFEFF3),
    outline = Color(0xFF8A8C9C),
    outlineVariant = Color(0xFFD6D7E0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF5A4E),
    onPrimary = Color(0xFF3B0000),
    primaryContainer = Color(0xFF8C0A0A),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFB8BBC8),
    onSecondary = Color(0xFF23252F),
    tertiary = Color(0xFFD48AF0),
    background = Color(0xFF0E0E13),
    onBackground = Color(0xFFEDEDF2),
    surface = Color(0xFF17171F),
    onSurface = Color(0xFFEDEDF2),
    surfaceVariant = Color(0xFF242430),
    onSurfaceVariant = Color(0xFFB9BACB),
    surfaceContainer = Color(0xFF1D1D27),
    outline = Color(0xFF7B7D8E),
    outlineVariant = Color(0xFF34354A),
)

private val BaseTypography = Typography()

private val RaceTypography = BaseTypography.copy(
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Black),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
)

/** The purple timing screens use for the fastest lap. */
val FastestPurple = Color(0xFFA020F0)

@Composable
fun RaceHubTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = RaceTypography, content = content)
}
