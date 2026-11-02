package com.ecofit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Black = Color(0xFF000000)
val Ink = Color(0xFF0B0D10)
val Panel = Color(0xFF17191D)
val Paper = Color(0xFFFFFFFF)
val Ice = Color(0xFF7CC8FF)
val Muted = Color(0xFFA3ABB6)
val InkMuted = Color(0xFF3D4550)
val Amber = Color(0xFFFFB648)

// 예전 화면 코드 호환용
val EcoGreen = Ice
val EcoGreenDim = Color(0xFF3E6A8A)
val Charcoal = Ink
val Slate = Panel
val Mist = Muted
val Warn = Amber

private val scheme = darkColorScheme(
    primary = Ice,
    onPrimary = Ink,
    background = Black,
    surface = Panel,
    onBackground = Paper,
    onSurface = Paper,
    secondary = Muted,
)

@Composable
fun EcoFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
