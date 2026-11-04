package com.ecofit.app.ui.report

import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
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
import com.ecofit.app.ui.Amount
import com.ecofit.app.ui.ProgressBar
import com.ecofit.app.ui.RingGauge
import com.ecofit.app.ui.Tile
import com.ecofit.app.ui.TileLabel
import com.ecofit.app.ui.TileTone
import com.ecofit.app.ui.countUp
import com.ecofit.app.ui.fmt
import com.ecofit.app.ui.signed
import com.ecofit.app.ui.theme.Amber
import com.ecofit.app.ui.theme.Ice
import com.ecofit.app.ui.theme.Ink
import com.ecofit.app.ui.theme.InkMuted
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

    if (trips.isEmpty()) {
        Tile(TileTone.Accent, Modifier.fillMaxSize()) {
            TileLabel("주행 후 리포트")
            Text(
                "아직 기록된 주행이 없어요\n한 번 다녀오면 여기에 리포트가 생겨요",
                fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 64.sp,
            )
        }
        return
    }

    val latest = trips.first()
    val s = vm.savings(latest)
    val score = EcoScore.of(latest)
    val recent = trips.take(6).reversed().map { it to vm.savings(it).savedWon }
    val maxWon = (recent.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    val regenWh = (s.regenKwh * 1000).fmt(0)
    val coachTip = if (latest.hardBrakeCount > 0)
        "급제동 ${latest.hardBrakeCount}회 · 조금 일찍 떼면 더 아낄 수 있어요"
    else
        "급제동 없이 부드럽게 달렸어요 · 이대로 유지하세요"

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile(TileTone.Accent, Modifier.weight(2f).fillMaxHeight()) {
                TileLabel("방금 끝난 주행 · ${timeFmt.format(Date(latest.startTimeMs))}\n회생 제동으로 아낀 돈")
                Column {
                    Amount("%,d".format(countUp(s.savedWon)), "원", 150.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${latest.distanceKm.fmt(1)}km · ${latest.durationSec / 60}분 · 되찾은 에너지 ${regenWh}Wh" +
                            (s.vsBaselinePercent?.let { " · 전비 ${it.signed()}%" } ?: ""),
                        fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Tile(TileTone.Light, Modifier.weight(1f).fillMaxHeight()) {
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
                    "급제동 ${latest.hardBrakeCount}회 · 급가속 ${latest.hardAccelCount}회",
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
                    recent.forEach { (t, won) ->
                        Column(
                            Modifier.weight(1f).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("${won}원", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(0.55f)
                                        .fillMaxHeight((won.toFloat() / maxWon).coerceAtLeast(0.04f))
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (t == latest) Ice else Ink)
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
