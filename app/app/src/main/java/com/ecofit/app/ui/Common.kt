package com.ecofit.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecofit.app.ui.theme.Ice
import com.ecofit.app.ui.theme.Ink
import com.ecofit.app.ui.theme.Muted
import com.ecofit.app.ui.theme.Panel
import com.ecofit.app.ui.theme.Paper

enum class TileTone { Accent, Light, Dark }

@Composable
fun Tile(
    tone: TileTone,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bg = when (tone) {
        TileTone.Accent -> Ice
        TileTone.Light -> Paper
        TileTone.Dark -> Panel
    }
    val fg = if (tone == TileTone.Dark) Paper else Ink
    CompositionLocalProvider(LocalContentColor provides fg) {
        Column(
            modifier
                .clip(RoundedCornerShape(28.dp))
                .background(bg)
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            content = content,
        )
    }
}

@Composable
fun TileLabel(text: String, color: Color = LocalContentColor.current.copy(alpha = 0.72f)) {
    Text(text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = color, lineHeight = 34.sp)
}

// 큰 숫자 + 작은 단위
@Composable
fun Amount(value: String, unit: String, size: TextUnit = 96.sp) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontSize = size, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)) { append(value) }
            if (unit.isNotEmpty()) {
                withStyle(SpanStyle(fontSize = size * 0.36f, fontWeight = FontWeight.Bold)) { append(" $unit") }
            }
        },
        lineHeight = size,
    )
}

@Composable
fun ProgressBar(fraction: Float, track: Color, fill: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(5.dp))
                .background(fill)
        )
    }
}

// 아래는 예전 화면 코드 호환용
@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Tile(TileTone.Dark, modifier) { content() }
}

@Composable
fun Label(text: String) {
    Text(text, color = Muted, fontSize = 18.sp)
}

@Composable
fun BigNumber(value: String, unit: String = "") {
    Amount(value, unit, 56.sp)
}

@Composable
fun ScreenTitle(text: String) {
    Text(text, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(20.dp))
}

fun Float.fmt(digits: Int = 1): String = "%.${digits}f".format(this)
fun Int.won(): String = "%,d원".format(this)
fun Int.signed(): String = if (this >= 0) "+$this" else "$this"

// 급제동/급가속은 전비뿐 아니라 동승자 승차감에도 직결된다. 100점에서 이벤트당 감점.
fun comfortScore(hardBrakeCount: Int, hardAccelCount: Int): Int =
    (100 - hardBrakeCount * 8 - hardAccelCount * 5).coerceIn(0, 100)
