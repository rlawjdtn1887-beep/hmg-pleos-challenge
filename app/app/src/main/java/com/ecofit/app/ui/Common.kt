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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import kotlin.math.roundToInt

enum class TileTone { Accent, Light, Dark }

@Composable
fun Tile(
    tone: TileTone,
    modifier: Modifier = Modifier,
    // 내용이 적어 빈 공간이 크게 남는 타일에, 그 공백을 브랜드 마크로 아주 은은하게 채운다
    watermark: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bg = when (tone) {
        TileTone.Accent -> Ice
        TileTone.Light -> Paper
        TileTone.Dark -> Panel
    }
    val fg = if (tone == TileTone.Dark) Paper else Ink
    CompositionLocalProvider(LocalContentColor provides fg) {
        Box(
            modifier
                .clip(RoundedCornerShape(28.dp))
                .background(bg)
        ) {
            if (watermark) {
                EcoLogo(
                    size = 120.dp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).alpha(0.07f),
                )
            }
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                content = content,
            )
        }
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

// 배터리 모양 그대로 그려서 숫자보다 한눈에 들어오게 한다. 막대그래프보다 "배터리"라는 걸 바로 알아볼 수 있다
@Composable
fun BatteryGauge(fraction: Float, fill: Color, modifier: Modifier = Modifier) {
    val outline = LocalContentColor.current.copy(alpha = 0.35f)
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), animationSpec = tween(COUNT_UP_MS, easing = FastOutSlowInEasing), label = "battery")
    Canvas(modifier.fillMaxWidth().height(48.dp)) {
        val nubWidth = size.height * 0.16f
        val nubHeight = size.height * 0.42f
        val gap = 6.dp.toPx()
        val bodyWidth = size.width - nubWidth - gap
        val strokeW = 5.dp.toPx()
        val corner = CornerRadius(12.dp.toPx())

        drawRoundRect(
            color = outline,
            size = Size(bodyWidth, size.height),
            cornerRadius = corner,
            style = Stroke(strokeW),
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(bodyWidth + gap, (size.height - nubHeight) / 2),
            size = Size(nubWidth, nubHeight),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        val pad = strokeW * 1.4f
        val fillWidth = (bodyWidth - pad * 2) * f
        if (fillWidth > 0f) {
            drawRoundRect(
                color = fill,
                topLeft = Offset(pad, pad),
                size = Size(fillWidth, size.height - pad * 2),
                cornerRadius = CornerRadius((corner.x - pad).coerceAtLeast(0f)),
            )
        }
    }
}

// 급제동 횟수처럼 "한눈에 위험 신호"를 줘야 하는 값 하나에만 쓴다.
// 모든 줄에 아이콘을 넣으면 오히려 복잡해 보여서, 가장 중요한 신호 하나만 그림으로 강조한다
@Composable
fun WarningTriangle(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(28.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 2.6.dp.toPx()
        val path = Path().apply {
            moveTo(w / 2, h * 0.06f)
            lineTo(w * 0.96f, h * 0.92f)
            lineTo(w * 0.04f, h * 0.92f)
            close()
        }
        drawPath(path, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(tint, Offset(w / 2, h * 0.38f), Offset(w / 2, h * 0.64f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawCircle(tint, radius = strokeW * 0.65f, center = Offset(w / 2, h * 0.78f))
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

// 1km 미만 거리는 정수로 반올림하면 "0km"로 뭉개져서, 그럴 땐 m 단위로 보여준다.
// (margin처럼 음수일 수도 있는 값이라 크기 기준으로 판단한다)
fun Float.kmOrMParts(): Pair<String, String> =
    if (kotlin.math.abs(this) < 1f) (this * 1000).roundToInt().toString() to "m" else fmt(0) to "km"

fun Float.fmtKmOrM(): String = kmOrMParts().let { (v, u) -> "$v$u" }

// 음성 안내용. "0킬로미터"로 들리는 걸 막는다
fun Float.fmtKmOrMKorean(): String =
    if (kotlin.math.abs(this) < 1f) "${(this * 1000).roundToInt()}미터" else "${fmt(0)}킬로미터"

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
