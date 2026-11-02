package com.ecofit.app.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecofit.app.EcoFitApp
import com.ecofit.app.data.local.toModel
import com.ecofit.app.data.model.TripData
import com.ecofit.app.ui.Amount
import com.ecofit.app.ui.Tile
import com.ecofit.app.ui.TileLabel
import com.ecofit.app.ui.TileTone
import com.ecofit.app.ui.fmt
import com.ecofit.app.ui.signed
import com.ecofit.app.ui.theme.Amber
import com.ecofit.app.ui.theme.Ice
import com.ecofit.app.ui.theme.Ink
import com.ecofit.app.ui.theme.InkMuted
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportViewModel : ViewModel() {
    private val app = EcoFitApp.instance
    val trips = app.db.tripDao().observeAll()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun savings(t: TripData) = app.savings.calculate(t)
}

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
    val recent = trips.take(6).reversed().map { it to vm.savings(it).savedWon }
    val maxWon = (recent.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile(TileTone.Accent, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel("방금 끝난 주행 · ${timeFmt.format(Date(latest.startTimeMs))}\n회생 제동으로 아낀 돈")
                Column {
                    Amount("%,d".format(s.savedWon), "원", 130.sp)
                    s.vsBaselinePercent?.let {
                        Text("기준 대비 전비 ${it.signed()}%", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Tile(TileTone.Dark, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel("되찾은 에너지", color = Ice)
                Column {
                    Amount((s.regenKwh * 1000).fmt(0), "Wh", 96.sp)
                    Text("${s.savedKm.fmt(1)}km 더 달릴 수 있는 양", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Tile(TileTone.Dark, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel("다음엔 이렇게", color = Amber)
                Text(
                    if (latest.hardBrakeCount > 0)
                        "급제동 ${latest.hardBrakeCount}회\n조금 일찍 떼면\n더 아낄 수 있어요"
                    else
                        "급제동 없이\n부드럽게 달렸어요\n이대로 유지하세요",
                    fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 50.sp,
                )
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile(TileTone.Light, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel("이번 주행")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${latest.distanceKm.fmt(1)}km · ${latest.durationSec / 60}분", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("전비 ${s.efficiencyKmPerKwh?.fmt(1) ?: "—"} km/kWh", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("급제동 ${latest.hardBrakeCount}회 · 급가속 ${latest.hardAccelCount}회", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = InkMuted)
                }
            }
            Tile(TileTone.Light, Modifier.weight(2f).fillMaxHeight()) {
                TileLabel("지난 주행")
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
