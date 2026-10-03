package com.glowcare.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

// Light palette: warm cream background, soft pink/lilac accent, white cards, dark text.
private val LightColors = lightColorScheme(
    primary = Color(0xFFC4707F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF9DEE3),
    onPrimaryContainer = Color(0xFF5A2430),
    secondary = Color(0xFF9A7BA6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEBDDF3),
    onSecondaryContainer = Color(0xFF3B2B44),
    tertiary = Color(0xFF8FA98A),
    background = Color(0xFFFBF6F2),
    onBackground = Color(0xFF2B2226),
    surface = Color.White,
    onSurface = Color(0xFF2B2226),
    surfaceVariant = Color(0xFFF4ECE7),
    onSurfaceVariant = Color(0xFF6E5F66),
    outline = Color(0xFFD9CCC7)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8A9B4),
    onPrimary = Color(0xFF49121C),
    primaryContainer = Color(0xFF5A2E38),
    onPrimaryContainer = Color(0xFFF9DEE3),
    secondary = Color(0xFFC4AED0),
    onSecondary = Color(0xFF33253C),
    secondaryContainer = Color(0xFF4A3B54),
    onSecondaryContainer = Color(0xFFEBDDF3),
    tertiary = Color(0xFFAFC2A8),
    background = Color(0xFF191416),
    onBackground = Color(0xFFEEE5E1),
    surface = Color(0xFF211B1E),
    onSurface = Color(0xFFEEE5E1),
    surfaceVariant = Color(0xFF2E2629),
    onSurfaceVariant = Color(0xFFC0B2B6),
    outline = Color(0xFF4A3E42)
)

val GlowTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

@Composable
fun GlowCareTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GlowTypography,
        content = content
    )
}
