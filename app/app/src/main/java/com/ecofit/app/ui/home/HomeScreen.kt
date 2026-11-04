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
import com.ecofit.app.domain.TripSavings
import com.ecofit.app.ui.Amount
import com.ecofit.app.ui.ProgressBar
import com.ecofit.app.ui.RingGauge
import com.ecofit.app.ui.Tile
import com.ecofit.app.ui.TileLabel
import com.ecofit.app.ui.TileTone
import com.ecofit.app.ui.fmt
import com.ecofit.app.ui.signed
import com.ecofit.app.ui.theme.Ice
import com.ecofit.app.ui.theme.Ink
import com.ecofit.app.ui.theme.InkMuted
import com.ecofit.app.ui.theme.Paper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

// SavingsCalculator의 커피 단가와 같게 유지
private const val COFFEE_WON = 4500

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
        if (isDemo.value) app.vehicleRepo.stopDemo() else app.vehicleRepo.startDemo()
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

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile(TileTone.Accent, Modifier.weight(2f).fillMaxHeight()) {
                TileLabel("이번 주 회생 제동으로 아낀 전기요금")
                Column {
                    Amount("%,d".format(s.savedWon), "원", 150.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (tripCount == 0) "주행을 시작하면 여기에 쌓여요" else "이번 주 주행 ${tripCount}회",
                            fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        // 운전 없이 전체 흐름을 보여 주는 시연용 버튼
                        Box(
                            Modifier
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
            Tile(TileTone.Light, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel("이번 주 에코 점수")
                RingGauge(
                    fraction = (avgScore ?: 0) / 100f,
                    track = Ink.copy(alpha = 0.08f),
                    fill = Ice,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Amount(avgScore?.toString() ?: "—", "점", 92.sp)
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
                TileLabel("이만큼이면")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${s.savedKm.fmt(1)}km를\n공짜로 달린 셈",
                        fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 56.sp,
                    )
                    Text(
                        if (s.coffeeCups >= 1f) "커피 ${s.coffeeCups.fmt(1)}잔 값"
                        else "커피 한 잔까지 ${"%,d".format(COFFEE_WON - s.savedWon)}원",
                        fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Ice,
                    )
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
