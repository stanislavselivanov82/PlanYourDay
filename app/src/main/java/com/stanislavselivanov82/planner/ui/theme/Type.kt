package com.stanislavselivanov82.planner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

private val defaults = Typography()

val PlannerTypography = Typography(
    displayLarge = defaults.displayLarge.copy(fontFamily = FontFamily.SansSerif),
    displayMedium = defaults.displayMedium.copy(fontFamily = FontFamily.SansSerif),
    displaySmall = defaults.displaySmall.copy(fontFamily = FontFamily.SansSerif),
    headlineLarge = defaults.headlineLarge.copy(fontFamily = FontFamily.SansSerif),
    headlineMedium = defaults.headlineMedium.copy(fontFamily = FontFamily.SansSerif),
    headlineSmall = defaults.headlineSmall.copy(fontFamily = FontFamily.SansSerif),
    titleLarge = defaults.titleLarge.copy(fontFamily = FontFamily.SansSerif),
    titleMedium = defaults.titleMedium.copy(fontFamily = FontFamily.SansSerif),
    titleSmall = defaults.titleSmall.copy(fontFamily = FontFamily.SansSerif),
    bodyLarge = defaults.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
    bodyMedium = defaults.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
    bodySmall = defaults.bodySmall.copy(fontFamily = FontFamily.SansSerif),
    labelLarge = defaults.labelLarge.copy(fontFamily = FontFamily.SansSerif),
    labelMedium = defaults.labelMedium.copy(fontFamily = FontFamily.SansSerif),
    labelSmall = defaults.labelSmall.copy(fontFamily = FontFamily.SansSerif),
)
