package com.ecofit.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
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

// 0~1 비율을 원형 링으로. 가운데에 내용을 넣는다
@Composable
fun RingGauge(
    fraction: Float,
    track: Color,
    fill: Color,
    modifier: Modifier = Modifier,
    stroke: Dp = 22.dp,
    content: @Composable () -> Unit,
) {
    // 처음 보일 때 0에서 차오르고, 값이 바뀌면 그쪽으로 움직인다
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val f by animateFloatAsState(
        if (shown) fraction.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(COUNT_UP_MS, easing = FastOutSlowInEasing),
        label = "ring",
    )
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2
            val arcSize = Size(size.width - w, size.height - w)
            drawArc(
                color = track, startAngle = 135f, sweepAngle = 270f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = w, cap = StrokeCap.Round),
            )
            if (f > 0f) drawArc(
                color = fill, startAngle = 135f, sweepAngle = 270f * f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = w, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

const val COUNT_UP_MS = 900

// 화면에 처음 보일 때 0에서 목표 숫자까지 올라간다. 숫자가 바뀌면 거기서 이어서 움직인다
@Composable
fun countUp(target: Int): Int {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val v by animateIntAsState(
        if (shown) target else 0,
        animationSpec = tween(COUNT_UP_MS, easing = FastOutSlowInEasing),
        label = "countUp",
    )
    return v
}

// 1분 미만이면 초로, 그 이상이면 분으로
fun Long.durationText(): String = if (this < 60) "${this}초" else "${this / 60}분"
