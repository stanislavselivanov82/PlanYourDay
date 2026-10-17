package com.stanislavselivanov82.planner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode { DARK, LIGHT, SYSTEM }

private val DarkColors = darkColorScheme(
    primary = PlannerAccent,
    onPrimary = Color(0xFF101A44),
    primaryContainer = Color(0xFF283563),
    onPrimaryContainer = Color(0xFFDDE3FF),
    secondary = Color(0xFFBCC5EB),
    background = PlannerBackground,
    onBackground = PlannerText,
    surface = PlannerSurface,
    onSurface = PlannerText,
    surfaceVariant = Color(0xFF282C36),
    onSurfaceVariant = Color(0xFFC2C6D2),
    outline = Color(0xFF858A99),
)

private val LightColors = lightColorScheme(
    primary = PlannerLightAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE3FF),
    onPrimaryContainer = Color(0xFF142254),
    background = PlannerLightBackground,
    onBackground = PlannerLightText,
    surface = PlannerLightSurface,
    onSurface = PlannerLightText,
    surfaceVariant = Color(0xFFE6E8F0),
    onSurfaceVariant = Color(0xFF444956),
)

private val PlannerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

@Composable
fun PlanYourDayTheme(
    mode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            if (activity != null) {
                activity.window.statusBarColor = colors.background.toArgb()
                activity.window.navigationBarColor = colors.background.toArgb()
                WindowCompat.getInsetsController(activity.window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = PlannerTypography,
        shapes = PlannerShapes,
        content = content,
    )
}
