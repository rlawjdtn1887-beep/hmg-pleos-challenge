package com.ecofit.app.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecofit.app.EcoFitApp
import com.ecofit.app.data.local.toModel
import com.ecofit.app.data.model.TripData
import com.ecofit.app.ui.BigNumber
import com.ecofit.app.ui.Card
import com.ecofit.app.ui.Label
import com.ecofit.app.ui.ScreenTitle
import com.ecofit.app.ui.fmt
import com.ecofit.app.ui.won
import com.ecofit.app.ui.theme.EcoGreen
import com.ecofit.app.ui.theme.Mist
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

private val fmtTime = SimpleDateFormat("M/d HH:mm", Locale.KOREA)

@Composable
fun ReportScreen(vm: ReportViewModel = viewModel()) {
    val trips by vm.trips.collectAsStateWithLifecycle()

    Column {
        ScreenTitle("주행 후 리포트")
        if (trips.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text("아직 기록된 주행이 없어요. 한 번 다녀오면 여기에 리포트가 생겨요.", fontSize = 22.sp, color = Mist)
            }
            return
        }

        val latest = trips.first()
        val s = vm.savings(latest)
        Card(Modifier.fillMaxWidth()) {
            Label("최근 주행 · ${fmtTime.format(Date(latest.startTimeMs))} · ${latest.distanceKm.fmt(1)}km · ${latest.durationSec / 60}분")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                Column {
                    Label("회생으로 아낀 돈")
                    Text(s.savedWon.won(), fontSize = 48.sp, color = EcoGreen)
                }
                Column {
                    Label("전비")
                    BigNumber(s.efficiencyKmPerKwh?.fmt(1) ?: "—", "km/kWh")
                }
                Column {
                    Label("기준 대비")
                    BigNumber(s.vsBaselinePercent?.let { "${if (it >= 0) "+" else ""}$it" } ?: "—", "%")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "= 커피 ${s.coffeeCups.fmt(1)}잔 · ${s.savedKm.fmt(1)}km 더 감 · 급제동 ${latest.hardBrakeCount}회 · 급가속 ${latest.hardAccelCount}회",
                fontSize = 20.sp,
            )
        }

        Spacer(Modifier.height(20.dp))
        Label("지난 주행")
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(trips.drop(1)) { t ->
                val ts = vm.savings(t)
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "${fmtTime.format(Date(t.startTimeMs))}  ·  ${t.distanceKm.fmt(1)}km  ·  ${ts.savedWon.won()}  ·  ${ts.efficiencyKmPerKwh?.fmt(1) ?: "—"} km/kWh",
                        fontSize = 20.sp,
                    )
                }
            }
        }
    }
}
