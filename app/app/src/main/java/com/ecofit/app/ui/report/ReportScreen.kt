package com.ecofit.app.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecofit.app.EcoFitApp
import com.ecofit.app.data.local.toModel
import com.ecofit.app.data.model.TripData
import com.ecofit.app.domain.Coaching
import com.ecofit.app.domain.EcoScore
import com.ecofit.app.ui.Amount
import com.ecofit.app.ui.EcoLogo
import com.ecofit.app.ui.ProgressBar
import com.ecofit.app.ui.RingGauge
import com.ecofit.app.ui.Tile
import com.ecofit.app.ui.TileLabel
import com.ecofit.app.ui.TileTone
import com.ecofit.app.ui.comfortScore
import com.ecofit.app.ui.countUp
import com.ecofit.app.ui.durationText
import com.ecofit.app.ui.fmt
import com.ecofit.app.ui.signed
import com.ecofit.app.ui.tour.tourTarget
import com.ecofit.app.ui.theme.Amber
import com.ecofit.app.ui.theme.Ice
import com.ecofit.app.ui.theme.Ink
import com.ecofit.app.ui.theme.InkMuted
import com.ecofit.app.ui.theme.Panel
import com.ecofit.app.ui.theme.Muted
import com.ecofit.app.ui.theme.Paper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class ReportViewModel : ViewModel() {
    private val app = EcoFitApp.instance
    val trips = app.db.tripDao().observeAll()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun savings(t: TripData) = app.savings.calculate(t)
}

// DriveScreen의 목표와 같게 유지
private const val TARGET_KWH = 0.3f

private val timeFmt = SimpleDateFormat("M/d HH:mm", Locale.KOREA)
private val dayFmt = SimpleDateFormat("M/d", Locale.KOREA)

@Composable
fun ReportScreen(vm: ReportViewModel = viewModel()) {
    val trips by vm.trips.collectAsStateWithLifecycle()
    val app = EcoFitApp.instance

    // 요약을 다 말하기 전에 다른 화면으로 넘어가면(예: 홈 눌러서 데모 바로 시작) 뒤따라오지 않게 끊는다.
    // owner로 "내가 시킨 말일 때만" 끊어서, 이 화면이 사라지는 타이밍이 다음 화면 발화 시작보다
    // 늦게 잡혀도 막 시작된 다음 화면 말까지 같이 끊어버리는 일이 없게 한다
    val ttsOwner = remember { Any() }
    DisposableEffect(Unit) { onDispose { app.ttsRepo.stopIfOwnedBy(ttsOwner) } }

    if (trips.isEmpty()) {
        EmptyReport(onDemo = { app.vehicleRepo.startDemo() })
        return
    }

    val latest = trips.first()

    // 방금 끝난 주행 결과를 음성으로 요약. latest가 바뀔 때만(= 새 주행이 끝났을 때만) 말하고,
    // 그래프에서 지난 주행을 눌러봐도(selectedStartTimeMs 변경) 다시 말하지 않는다
    LaunchedEffect(latest.startTimeMs) {
        val ls = vm.savings(latest)
        val extraKm = Coaching.extraKmWithoutHardBrakes(latest)
        val tip = if (latest.hardBrakeCount > 0)
            "급제동 ${latest.hardBrakeCount}번이 없었다면 약 ${extraKm.fmt(1)}킬로미터 더 갈 수 있었어요"
        else
            "급제동 없이 부드럽게 달렸어요"
        val charge = if (Coaching.isLowBattery(latest.endSocPercent)) " 배터리가 부족해요. 충전소 찾기를 눌러 주세요." else ""
        // 주행이 끝났을 때 하는 요약이라 말하던 게 있어도 끊고 말한다
        app.ttsRepo.speak(
            "주행이 끝났어요. ${latest.distanceKm.fmt(1)}킬로미터를 달리면서 ${ls.savedWon}원을 아꼈어요. " +
                "에코 점수 ${EcoScore.of(latest)}점, $tip.$charge",
            interrupt = true,
            owner = ttsOwner,
        )
    }

    // 리포트 그래프 막대를 눌러서 그 주행을 대신 보여준다. null이면 방금 끝난 주행
    var selectedStartTimeMs by remember { mutableStateOf<Long?>(null) }
    val shown = selectedStartTimeMs?.let { ms -> trips.find { it.startTimeMs == ms } } ?: latest
    val isLatestShown = shown.startTimeMs == latest.startTimeMs
    val s = vm.savings(shown)
    val score = EcoScore.of(shown)
    val recent = trips.take(6).reversed().map { Triple(it, vm.savings(it).savedWon, EcoScore.of(it)) }
    val maxWon = (recent.maxOf { it.second }).coerceAtLeast(1)

    val regenWh = (s.regenKwh * 1000).fmt(0)
    val coachTip = if (shown.hardBrakeCount > 0)
        "급제동 ${shown.hardBrakeCount}회 · 없었다면 약 ${Coaching.extraKmWithoutHardBrakes(shown).fmt(1)}km 더 갈 수 있었어요"
    else
        "급제동 없이 부드럽게 달렸어요 · 이대로 유지하세요"

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile(TileTone.Accent, Modifier.weight(2f).fillMaxHeight()) {
                TileLabel(
                    (if (isLatestShown) "방금 끝난 주행" else "선택한 주행") +
                        " · ${timeFmt.format(Date(shown.startTimeMs))}\n회생 제동으로 아낀 돈"
                )
                Column {
                    Amount("%,d".format(countUp(s.savedWon)), "원", 150.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${shown.distanceKm.fmt(1)}km · ${shown.durationSec.durationText()} · 되찾은 에너지 ${regenWh}Wh" +
                                (s.vsBaselinePercent?.let { " · 전비 ${it.signed()}%" } ?: ""),
                            fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        // 배터리가 적게 남은 채로 끝났으면 바로 충전소를 찾게 한다
                        if (Coaching.isLowBattery(shown.endSocPercent)) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Ink)
                                    .clickable {
                                        app.ttsRepo.speak("근처 충전소를 찾아드릴게요", interrupt = true, owner = ttsOwner)
                                        app.naviRepo.showNearbyChargingStations()
                                    }
                                    .padding(horizontal = 22.dp, vertical = 12.dp),
                            ) {
                                Text("⚡ 충전소 찾기", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ice)
                            }
                        }
                    }
                }
            }
            Tile(TileTone.Light, Modifier.weight(1f).fillMaxHeight().tourTarget("report.score")) {
                TileLabel("에코 점수")
                RingGauge(
                    fraction = score / 100f,
                    track = Ink.copy(alpha = 0.08f),
                    fill = Ice,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Amount("${countUp(score)}", "점", 92.sp)
                        Text(EcoScore.label(score), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = InkMuted)
                    }
                }
                Text(
                    "급제동 ${shown.hardBrakeCount}회 · 급가속 ${shown.hardAccelCount}회",
                    fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = InkMuted,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                Text(
                    "동승자 승차감 ${comfortScore(shown.hardBrakeCount, shown.hardAccelCount)}점",
                    fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = InkMuted,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile(TileTone.Dark, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel("되찾은 에너지", color = Ice)
                Column {
                    Amount("${countUp((s.regenKwh * 1000).roundToInt())}", "Wh", 88.sp)
                    Spacer(Modifier.height(14.dp))
                    ProgressBar(s.regenKwh / TARGET_KWH, track = Ice.copy(alpha = 0.18f), fill = Ice)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "목표 ${(TARGET_KWH * 1000).toInt()}Wh · ${s.savedKm.fmt(1)}km 더 달릴 수 있는 양",
                        fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
                HorizontalDivider(color = Paper.copy(alpha = 0.12f))
                Column {
                    TileLabel("다음엔 이렇게", color = Amber)
                    Spacer(Modifier.height(6.dp))
                    Text(coachTip, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 38.sp)
                }
            }
            Tile(TileTone.Light, Modifier.weight(1.5f).fillMaxHeight()) {
                TileLabel("지난 주행")
                Row(
                    Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    recent.forEach { (t, won, tScore) ->
                        val isSelected = t.startTimeMs == shown.startTimeMs
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedStartTimeMs = t.startTimeMs },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "${won}원", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Ice else Ink,
                            )
                            Text("${tScore}점", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = InkMuted)
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(0.55f)
                                        .fillMaxHeight((won.toFloat() / maxWon).coerceAtLeast(0.04f))
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Ice else Ink)
                                        .then(
                                            if (isSelected) Modifier.border(2.dp, Ink, RoundedCornerShape(10.dp))
                                            else Modifier
                                        )
                                )
                            }
                            Text(dayFmt.format(Date(t.startTimeMs)), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = InkMuted)
                        }
                    }
                }
            }
        }
    }
}

// 기록이 없을 때. 링 안에 로고를 두고, 데모로 바로 이어지게 한다
@Composable
private fun EmptyReport(onDemo: () -> Unit) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            Modifier
                .weight(2f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(Panel)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            0f to Ice.copy(alpha = 0.22f), 1f to Color.Transparent,
                            center = Offset(size.width / 2f, size.height * 0.42f),
                            radius = size.minDimension * 0.6f,
                        )
                    )
                }
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            TileLabel("주행 후 리포트", color = Paper.copy(alpha = 0.72f))
            RingGauge(
                fraction = 0f,
                track = Ice.copy(alpha = 0.16f),
                fill = Ice,
                modifier = Modifier.weight(1f).widthIn(max = 320.dp).padding(vertical = 8.dp),
                stroke = 26.dp,
            ) {
                EcoLogo(120.dp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("아직 기록된 주행이 없어요", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Paper)
                Spacer(Modifier.height(10.dp))
                Text(
                    "D로 출발하고 P로 세우면\n여기에 아낀 돈과 에코 점수가 쌓여요",
                    fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Muted,
                    textAlign = TextAlign.Center, lineHeight = 32.sp,
                )
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Ice)
                    .clickable(onClick = onDemo)
                    .padding(28.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(28.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.15f, 0f)
                            lineTo(size.width, size.height / 2f)
                            lineTo(size.width * 0.15f, size.height)
                            close()
                        }
                        drawPath(path, Ice)
                    }
                }
                Column {
                    Text("데모 주행 보기", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "약 45초 · 운전 없이 전체 흐름을 볼 수 있어요",
                        fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = 0.75f),
                    )
                }
            }
            Tile(TileTone.Light, Modifier.weight(1f).fillMaxWidth()) {
                TileLabel("리포트에 담기는 것")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("회생 제동으로 아낀 돈", "에코 점수 0~100", "되찾은 에너지와 코칭", "지난 주행 비교 그래프").forEach {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Ice))
                            Spacer(Modifier.width(14.dp))
                            Text(it, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
