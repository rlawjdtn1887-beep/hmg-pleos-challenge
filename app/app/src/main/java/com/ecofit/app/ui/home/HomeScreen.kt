package com.ecofit.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ecofit.app.domain.EcoScore
import com.ecofit.app.domain.MoneyUnits
import com.ecofit.app.domain.TripSavings
import com.ecofit.app.ui.Amount
import com.ecofit.app.ui.ProgressBar
import com.ecofit.app.ui.RingGauge
import com.ecofit.app.ui.Tile
import com.ecofit.app.ui.TileLabel
import com.ecofit.app.ui.TileTone
import com.ecofit.app.ui.countUp
import com.ecofit.app.ui.tour.tourTarget
import com.ecofit.app.ui.fmt
import com.ecofit.app.ui.signed
import com.ecofit.app.ui.theme.Ice
import com.ecofit.app.ui.theme.Ink
import com.ecofit.app.ui.theme.InkMuted
import com.ecofit.app.ui.theme.Paper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

private const val WEEKS_PER_YEAR = 52

// 인사는 앱을 켠 뒤 한 번만
private var greeted = false

data class WeeklySummary(val tripCount: Int, val savings: TripSavings, val avgScore: Int?)

class HomeViewModel : ViewModel() {
    private val app = EcoFitApp.instance
    private val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000

    val weekly = app.db.tripDao().observeSince(weekAgo)
        .map { list -> list.map { it.toModel() } }
        .map { trips -> WeeklySummary(trips.size, app.savings.weekly(trips), averageScore(trips)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklySummary(0, app.savings.weekly(emptyList()), null))

    private fun averageScore(trips: List<TripData>): Int? =
        if (trips.isEmpty()) null else trips.map { EcoScore.of(it) }.average().roundToInt()

    val snapshot = app.vehicleRepo.snapshot
    val isDemo = app.vehicleRepo.isDemo

    fun toggleDemo() {
        if (isDemo.value) {
            app.vehicleRepo.stopDemo()
        } else {
            app.ttsRepo.speak("데모 주행을 시작합니다")
            app.vehicleRepo.startDemo()
        }
    }
}

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val weekly by vm.weekly.collectAsStateWithLifecycle()
    val tripCount = weekly.tripCount
    val s = weekly.savings
    val avgScore = weekly.avgScore
    val snap by vm.snapshot.collectAsStateWithLifecycle()
    val isDemo by vm.isDemo.collectAsStateWithLifecycle()

    val soc = snap.batterySocPercent
    val range = snap.remainingRangeKm?.takeIf { it > 0f }

    // 말하다가 다른 화면으로 넘어가면 뒤따라오지 않게 끊는다
    DisposableEffect(Unit) { onDispose { EcoFitApp.instance.ttsRepo.stopSpeaking() } }

    // 앱을 켠 뒤 처음 홈에 왔을 때 한 번만 인사한다. weekly는 Room 쿼리가 끝나기 전엔
    // 초기값(0건)이라 잠깐 기다렸다가 최신 값을 읽는다
    LaunchedEffect(Unit) {
        if (greeted) return@LaunchedEffect
        greeted = true
        delay(400)
        val w = vm.weekly.value
        val hello = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..10 -> "좋은 아침이에요."
            in 11..16 -> "좋은 오후예요."
            in 17..21 -> "좋은 저녁이에요."
            else -> "안녕하세요."
        }
        val text = if (w.tripCount == 0) {
            "$hello 에코핏이 회생 제동을 기록할게요."
        } else {
            "$hello 이번 주 ${"%,d".format(w.savings.savedWon)}원 아꼈어요. 오늘도 부드럽게 출발해요."
        }
        EcoFitApp.instance.ttsRepo.speak(text)
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile(TileTone.Accent, Modifier.weight(2f).fillMaxHeight().tourTarget("home.savings")) {
                TileLabel("이번 주 회생 제동으로 아낀 전기요금")
                Column {
                    Amount("%,d".format(countUp(s.savedWon)), "원", 150.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (tripCount == 0) "주행을 시작하면 여기에 쌓여요" else "이번 주 주행 ${tripCount}회",
                            fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        // 운전 없이 전체 흐름을 보여 주는 시연용 버튼
                        Box(
                            Modifier
                                .tourTarget("home.demo")
                                .clip(RoundedCornerShape(50))
                                .background(if (isDemo) Paper else Ink)
                                .clickable { vm.toggleDemo() }
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                        ) {
                            Text(
                                if (isDemo) "데모 중지" else "데모 주행 보기",
                                fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                color = if (isDemo) Ink else Ice,
                            )
                        }
                    }
                }
            }
            Tile(TileTone.Light, Modifier.weight(1f).fillMaxHeight().tourTarget("home.score")) {
                TileLabel("이번 주 에코 점수")
                RingGauge(
                    fraction = (avgScore ?: 0) / 100f,
                    track = Ink.copy(alpha = 0.08f),
                    fill = Ice,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Amount(avgScore?.let { "${countUp(it)}" } ?: "—", "점", 92.sp)
                        Text(
                            avgScore?.let { EcoScore.label(it) } ?: "첫 주행을 기다려요",
                            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = InkMuted,
                        )
                    }
                }
                Text(
                    s.efficiencyKmPerKwh?.let { "평균 전비 ${it.fmt(1)} km/kWh" + (s.vsBaselinePercent?.let { p -> " · 기준 대비 ${p.signed()}%" } ?: "") }
                        ?: "주행 ${tripCount}회 평균",
                    fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = InkMuted,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile(TileTone.Dark, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel("되찾은 에너지")
                Amount(s.regenKwh.fmt(2), "kWh", 96.sp)
            }
            Tile(TileTone.Dark, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel("이대로 달리면")
                if (tripCount == 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "아직 기록이 없어요",
                            fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 46.sp,
                        )
                        Text(
                            "주행을 시작하면 1년 환산이 여기 떠요",
                            fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Ice,
                        )
                    }
                } else {
                    // 이번 주(최근 7일) 아낀 돈을 그대로 연 단위로 늘린 값. 주행이 쌓일수록 정확해진다
                    val annualWon = s.savedWon * WEEKS_PER_YEAR
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "1년에 약\n${"%,d".format(countUp(annualWon))}원",
                            fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 50.sp,
                        )
                        Text(
                            MoneyUnits.describe(annualWon) ?: MoneyUnits.untilNext(annualWon),
                            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ice,
                        )
                        Text(
                            "이번 주 ${s.savedKm.fmt(1)}km를 공짜로 달린 셈",
                            fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = InkMuted,
                        )
                    }
                }
            }
            Tile(TileTone.Light, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel("지금 배터리")
                Column {
                    Amount(soc?.fmt(0) ?: "—", "%", 96.sp)
                    Spacer(Modifier.height(14.dp))
                    ProgressBar((soc ?: 0f) / 100f, track = Ink.copy(alpha = 0.1f), fill = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        range?.let { "남은 거리 ${it.fmt(0)}km" } ?: "남은 거리 확인 중",
                        fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = InkMuted,
                    )
                }
            }
        }
    }
}
