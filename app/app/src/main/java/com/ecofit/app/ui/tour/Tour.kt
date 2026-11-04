package com.ecofit.app.ui.tour

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecofit.app.ui.theme.Ice
import com.ecofit.app.ui.theme.Ink
import com.ecofit.app.ui.theme.InkMuted
import com.ecofit.app.ui.theme.Paper
import kotlin.math.roundToInt

data class TourStep(val target: String, val route: String, val title: String, val body: String)

// 처음 켰을 때 화면 위에 말풍선으로 하나씩 짚어 준다. 한 번 끝내면 다시 안 뜬다.
object Tour {
    val steps = listOf(
        TourStep("home.savings", "home", "이번 주에 얼마나 아꼈을까요?", "가속 페달을 떼면 회생 제동으로 전기를 되찾아요.\n그걸 전기요금으로 바꿔서 매주 쌓아 보여줘요."),
        TourStep("home.score", "home", "운전 습관은 에코 점수로", "급제동과 급가속을 줄일수록 점수가 올라가요.\n이번 주 주행들의 평균이에요."),
        TourStep("drive.savings", "drive", "달리는 동안 실시간으로", "기어를 D로 바꾸면 이 화면이 자동으로 열려요.\n회생할 때마다 금액이 오르고 코칭 문구가 바뀌어요."),
        TourStep("report.score", "report", "주행이 끝나면 리포트", "P로 세우면 자동으로 리포트가 열려요.\n아낀 돈, 에코 점수, 지난 주행 비교까지 한 번에."),
        TourStep("home.demo", "home", "운전 없이 보고 싶다면", "이 버튼을 누르면 46초짜리 데모 주행이 돌아요.\n주행 중 화면부터 리포트까지 전부 볼 수 있어요."),
    )

    val bounds = mutableStateMapOf<String, Rect>()
    var step by mutableIntStateOf(-1)
        private set

    val active get() = step in steps.indices
    val current get() = steps.getOrNull(step)

    fun startIfFirstRun(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_DONE, false)) step = 0
    }

    fun next(context: Context) {
        if (step + 1 < steps.size) step++ else finish(context)
    }

    fun finish(context: Context) {
        step = -1
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply()
    }

    private const val PREFS = "ecofit"
    private const val KEY_DONE = "tour_done"
}

// 안내 대상이 되는 타일에 붙인다. 위치를 기억해 뒀다가 말풍선이 가리킨다
fun Modifier.tourTarget(id: String): Modifier = onGloballyPositioned { Tour.bounds[id] = it.boundsInRoot() }

@Composable
fun TourOverlay(onFinish: () -> Unit, onNext: () -> Unit) {
    val step = Tour.current ?: return
    val rect = Tour.bounds[step.target]
    val density = LocalDensity.current

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val pad = with(density) { 6.dp.toPx() }
        val corner = with(density) { 30.dp.toPx() }
        val stroke = with(density) { 3.dp.toPx() }

        // 어둡게 깔고 대상 타일만 구멍을 낸다
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color.Black.copy(alpha = 0.68f))
            if (rect != null) {
                val tl = Offset(rect.left - pad, rect.top - pad)
                val sz = Size(rect.width + pad * 2, rect.height + pad * 2)
                drawRoundRect(Color.Transparent, topLeft = tl, size = sz, cornerRadius = CornerRadius(corner), blendMode = BlendMode.Clear)
                drawRoundRect(Ice, topLeft = tl, size = sz, cornerRadius = CornerRadius(corner), style = Stroke(width = stroke))
            }
        }

        val cardW = 560.dp
        val cardWpx = with(density) { cardW.toPx() }
        val gap = with(density) { 18.dp.toPx() }
        val margin = with(density) { 16.dp.toPx() }
        val cardHGuess = with(density) { 300.dp.toPx() }

        // 타일 아래에 두고, 아래가 모자라면 위에, 위도 모자라면 가운데
        val x = (rect?.let { it.left + it.width / 2 - cardWpx / 2 } ?: (screenW / 2 - cardWpx / 2))
            .coerceIn(margin, (screenW - cardWpx - margin).coerceAtLeast(margin))
        val y: Float? = rect?.let {
            when {
                it.bottom + gap + cardHGuess < screenH -> it.bottom + gap
                it.top - gap - cardHGuess > 0f -> it.top - gap - cardHGuess
                else -> null
            }
        }

        Column(
            Modifier
                .then(if (y != null) Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) } else Modifier.align(Alignment.Center))
                .width(cardW)
                .clip(RoundedCornerShape(26.dp))
                .background(Paper)
                .padding(28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(50)).background(Ice.copy(alpha = 0.25f)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text("간단 사용 안내 · ${Tour.step + 1}/${Tour.steps.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
                Spacer(Modifier.weight(1f))
                Text("✕", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = InkMuted, modifier = Modifier.clickable(onClick = onFinish).padding(6.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(step.title, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Ink, lineHeight = 42.sp)
            Spacer(Modifier.height(10.dp))
            Text(step.body, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = InkMuted, lineHeight = 32.sp)
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("건너뛰기", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = InkMuted, modifier = Modifier.clickable(onClick = onFinish).padding(vertical = 8.dp))
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tour.steps.indices.forEach { i ->
                        Box(Modifier.size(10.dp).clip(CircleShape).background(if (i == Tour.step) Ink else Ink.copy(alpha = 0.18f)))
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(Ink).clickable(onClick = onNext).padding(horizontal = 26.dp, vertical = 14.dp),
                ) {
                    Text(if (Tour.step == Tour.steps.lastIndex) "시작하기" else "다음 →", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ice)
                }
            }
        }
    }
}

// 화면 전체를 감싸서 말풍선이 타일 위치(boundsInRoot)와 같은 좌표를 쓰게 한다
@Composable
fun TourHost(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) { content() }
}
