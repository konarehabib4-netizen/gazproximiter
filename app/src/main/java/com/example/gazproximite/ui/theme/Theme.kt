package com.example.gazproximite.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val YaGazColors = lightColorScheme(
    primary = YaDarkBlue,
    secondary = YaBlue,
    background = AppBackground,
    surface = CardBackground,
    onBackground = OxfordByNight,
    onSurface = OxfordByNight
)

@Composable
fun YaGazTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = YaGazColors,
        content = content
    )
}
