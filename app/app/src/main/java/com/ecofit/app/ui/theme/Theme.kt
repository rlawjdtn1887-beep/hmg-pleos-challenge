package com.ecofit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ecofit.app.R

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

// Pretendard (OFL). 차량 기본 글꼴보다 숫자와 한글이 또렷하다
val Pretendard = FontFamily(
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
    Font(R.font.pretendard_black, FontWeight.Black),
)

// 앱 안의 모든 Text가 이 글꼴을 쓴다
private val base = TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold)
private val typography = Typography(
    displayLarge = base, displayMedium = base, displaySmall = base,
    headlineLarge = base, headlineMedium = base, headlineSmall = base,
    titleLarge = base, titleMedium = base, titleSmall = base,
    bodyLarge = base, bodyMedium = base, bodySmall = base,
    labelLarge = base, labelMedium = base, labelSmall = base,
)

@Composable
fun EcoFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
