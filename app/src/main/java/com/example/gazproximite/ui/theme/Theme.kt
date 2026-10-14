package com.example.gazproximite.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Flamme,
    onPrimary = Color.White,
    primaryContainer = FlammeClair,
    onPrimaryContainer = FlammeFonce,
    secondary = BleuNuit,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E8F4),
    onSecondaryContainer = BleuNuit,
    background = FondClair,
    onBackground = BleuNuit,
    surface = SurfaceClaire,
    onSurface = BleuNuit,
    surfaceVariant = Color(0xFFF0F2F7),
    onSurfaceVariant = TexteSecondaire,
    outline = ContourClair,
    outlineVariant = ContourClair,
    error = RougeRupture
)

private val DarkColorScheme = darkColorScheme(
    primary = Flamme,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A2410),
    onPrimaryContainer = FlammeClair,
    secondary = Color(0xFFB9C4E0),
    onSecondary = BleuNuit,
    secondaryContainer = BleuNuitClair,
    onSecondaryContainer = Color.White,
    background = FondSombre,
    onBackground = Color(0xFFE8EBF2),
    surface = SurfaceSombre,
    onSurface = Color(0xFFE8EBF2),
    surfaceVariant = Color(0xFF222B40),
    onSurfaceVariant = Color(0xFFA6AEC0),
    outline = Color(0xFF34405A),
    outlineVariant = Color(0xFF2A3449),
    error = Color(0xFFFF6B6B)
)

// Coins arrondis utilisés dans toute l'application.
private val FormesGaz = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Les couleurs dynamiques d'Android 12 sont désactivées :
// l'application garde toujours ses propres couleurs.
@Composable
fun GazproximiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = FormesGaz,
        content = content
    )
}
