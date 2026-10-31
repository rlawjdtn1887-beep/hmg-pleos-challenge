package com.ecofit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EcoGreen = Color(0xFF2ED47A)
val EcoGreenDim = Color(0xFF1A7A48)
val Charcoal = Color(0xFF111418)
val Slate = Color(0xFF1C2128)
val Mist = Color(0xFF8B949E)
val Warn = Color(0xFFFFB020)

private val scheme = darkColorScheme(
    primary = EcoGreen,
    onPrimary = Charcoal,
    background = Charcoal,
    surface = Slate,
    onBackground = Color.White,
    onSurface = Color.White,
    secondary = Mist,
)

@Composable
fun EcoFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
