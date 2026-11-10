package com.ecofit.app.ui.drive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecofit.app.EcoFitApp
import com.ecofit.app.data.local.toModel
import com.ecofit.app.domain.Coaching
import com.ecofit.app.domain.RegenHistory
import com.ecofit.app.data.model.Gear
import com.ecofit.app.ui.Amount
import com.ecofit.app.ui.ProgressBar
import com.ecofit.app.ui.Tile
import com.ecofit.app.ui.TileLabel
import com.ecofit.app.ui.TileTone
import com.ecofit.app.ui.comfortScore
import com.ecofit.app.ui.durationText
import com.ecofit.app.ui.fmt
import com.ecofit.app.ui.signed
import com.ecofit.app.ui.tour.tourTarget
import com.ecofit.app.ui.theme.Amber
import com.ecofit.app.ui.theme.Ice
import com.ecofit.app.ui.theme.Ink
import com.ecofit.app.ui.theme.InkMuted
import com.ecofit.app.ui.theme.Paper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

// SavingsCalculator 단가와 같게 유지
private const val WON_PER_KWH = 350f
private const val TARGET_KWH = 0.3f
// 회생이 이 시간 동안 더 늘지 않으면 한 번이 끝난 것으로 본다
private const val EPISODE_END_MS = 1500L
private const val POPUP_MS = 2500L

class DriveViewModel : ViewModel() {
    private val app = EcoFitApp.instance

    // 직전 주행의 km당 금액. 같은 거리에서 얼마나 더 아끼는지 비교하는 기준
    val lastWonPerKm = app.db.tripDao().observeLatest()
        .map { RegenHistory.wonPerKm(it?.toModel(), WON_PER_KWH) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

// 누적은 TripRecorder가 하고 이 화면은 보여주기만 한다
@Composable
fun DriveScreen(vm: DriveViewModel = viewModel()) {
    val app = EcoFitApp.instance
    val snap by app.vehicleRepo.snapshot.collectAsStateWithLifecycle()
    val trip by app.recorder.live.collectAsStateWithLifecycle()
    val lastWonPerKm by vm.lastWonPerKm.collectAsStateWithLifecycle()
    val destKm by app.naviRepo.remainDistanceKm.collectAsStateWithLifecycle()

    // 주행 시작 시점의 목적지까지 거리를 한 번 붙잡아두고, 거기서 달린 거리(trip.distanceKm)를
    // 계속 빼서 보여준다. 내비 쪽 값은 15초마다만 갱신돼서 그대로 쓰면 뚝뚝 끊겨 보인다
    var destAtStart by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(destKm) {
        if (destAtStart == null) destAtStart = destKm?.takeIf { it > 0f }
    }
    val remainingToDestKm = destAtStart?.let { (it - trip.distanceKm).coerceAtLeast(0f) }

    val won = (trip.regenKwh * WON_PER_KWH).roundToInt()
    // 지난 주행이 지금 거리만큼 달렸을 때 아꼈을 금액과 비교
    val vsLast = lastWonPerKm?.takeIf { trip.distanceKm >= 0.1f }?.let { won - (it * trip.distanceKm).roundToInt() }
    val progress by animateFloatAsState((trip.regenKwh / TARGET_KWH).coerceIn(0f, 1f), label = "regen")
    // SDK 브레이크 값은 밟음 여부뿐이라 급제동은 감속도로 판단한 값을 쓴다
    val braking = (snap.brakePedalPercent ?: 0f) > 10f
    val hardBrake = trip.hardBraking

    // 회생 한 번이 끝날 때마다 그동안 모은 금액을 잠깐 띄운다
    var episodeStartKwh by remember { mutableFloatStateOf(trip.regenKwh) }
    var popupWon by remember { mutableIntStateOf(0) }
    var popupVisible by remember { mutableStateOf(false) }

    LaunchedEffect(trip.regenKwh) {
        delay(EPISODE_END_MS)
        val gained = ((trip.regenKwh - episodeStartKwh) * WON_PER_KWH).roundToInt()
        episodeStartKwh = trip.regenKwh
        if (gained >= 1) {
            popupWon = gained
            popupVisible = true
        }
    }
    // 운전 중 음성은 고쳐야 할 게 있을 때만. 급제동이 잡히면 바로 말하고 15초 안엔 다시 안 한다
    LaunchedEffect(hardBrake) {
        if (hardBrake) app.ttsRepo.speak("조금 더 일찍 페달을 떼 보세요", interrupt = true, key = "brake", minGapMs = 15_000)
    }
    // 달리는 중 배터리가 20% 아래로 내려가면 한 번만 알린다
    var lowBatterySpoken by remember { mutableStateOf(false) }
    LaunchedEffect(trip.active) { if (!trip.active) lowBatterySpoken = false }
    LaunchedEffect(snap.batterySocPercent) {
        if (trip.active && !lowBatterySpoken && Coaching.isLowBattery(snap.batterySocPercent)) {
            lowBatterySpoken = true
            app.ttsRepo.speak("배터리가 20% 아래예요. 세우면 가까운 충전소를 안내할게요", interrupt = true)
        }
    }
    LaunchedEffect(popupVisible, popupWon) {
        if (popupVisible) {
            delay(POPUP_MS)
            popupVisible = false
        }
    }

    val coach = when {
        snap.gear != Gear.D -> "출발하면 회생 제동을 기록해요"
        hardBrake -> "급제동이에요\n조금 더 일찍 페달을 떼보세요"
        braking -> "회생 중이에요\n페달을 부드럽게 유지하세요"
        else -> "앞차와 거리를 두고\n가속 페달을 떼면 회생이 시작돼요"
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(2f).fillMaxHeight().tourTarget("drive.savings")) {
                    // 가속 페달을 떼서 회생이 시작되는 순간 테두리가 은은하게 빛난다
                    val regenBorderAlpha by animateFloatAsState(if (braking) 1f else 0f, label = "regenBorder")
                    Tile(
                        TileTone.Accent,
                        Modifier
                            .fillMaxSize()
                            .border(3.dp, Ink.copy(alpha = regenBorderAlpha * 0.8f), RoundedCornerShape(28.dp)),
                    ) {
                        TileLabel("이번 주행에서 회생 제동으로 아낀 돈")
                        Column {
                            Amount("%,d".format(won), "원", 150.sp)
                            Spacer(Modifier.height(16.dp))
                            ProgressBar(progress, track = Ink.copy(alpha = 0.15f), fill = Ink)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${(trip.regenKwh * 1000).fmt(0)}Wh 되찾음 · 목표 ${(TARGET_KWH * 1000).toInt()}Wh" +
                                    (vsLast?.let { if (it >= 0) " · 지난 주행보다 ${it}원 더 아꼈어요" else " · 지난 주행보다 ${-it}원 적어요" } ?: ""),
                                fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    // Row 안의 Box라 implicit receiver가 겹쳐서 RowScope.AnimatedVisibility로 잘못 resolve됨.
                    // 명시적으로 top-level 함수를 가리켜서 그 모호성을 없앤다
                    androidx.compose.animation.AnimatedVisibility(
                        visible = braking,
                        modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
                        enter = fadeIn(), exit = fadeOut(),
                    ) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Ink.copy(alpha = 0.12f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Ink))
                            Spacer(Modifier.width(8.dp))
                            Text("회생 중", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                    }
                }
                Tile(TileTone.Light, Modifier.weight(1f).fillMaxHeight()) {
                    TileLabel("지금 속도")
                    Column {
                        Amount(snap.speedKmh?.fmt(0) ?: "—", "km/h", 96.sp)
                        Text("기어 ${snap.gear}", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = InkMuted)
                    }
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Tile(TileTone.Dark, Modifier.weight(2f).fillMaxHeight()) {
                    TileLabel(if (hardBrake) "주의" else "코칭", color = if (hardBrake) Amber else Ice)
                    // 코칭 문구는 짧을 때가 많아서 타일 아래쪽에 남는 여백에 목적지까지 거리를 같이 묶어 채운다
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            coach,
                            fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 58.sp,
                            color = if (hardBrake) Amber else Paper,
                        )
                        remainingToDestKm?.let {
                            Text("목적지까지 ${it.fmt(1)}km", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Ice)
                        }
                    }
                }
                Tile(TileTone.Light, Modifier.weight(1f).fillMaxHeight()) {
                    TileLabel("이번 주행")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${trip.distanceKm.fmt(1)}km · ${trip.durationSec.durationText()}", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text("급제동 ${trip.hardBrakeCount}회", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        // 소모량은 차가 전비를 줄 때만 쌓인다. 0이면 글자를 빼서 빈 숫자를 안 보여준다
                        val usedWh = (trip.usedKwh * 1000).fmt(0)
                        Text(
                            "승차감 ${comfortScore(trip.hardBrakeCount, trip.hardAccelCount)}점" + if (trip.usedKwh > 0f) " · 소모 ${usedWh}Wh" else "",
                            fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = InkMuted,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = popupVisible,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut(),
        ) {
            Text(
                "+%,d원".format(popupWon),
                modifier = Modifier
                    .clip(RoundedCornerShape(40.dp))
                    .background(Ink)
                    .padding(horizontal = 48.dp, vertical = 20.dp),
                color = Ice,
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
