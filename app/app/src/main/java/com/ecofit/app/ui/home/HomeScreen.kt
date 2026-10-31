package com.ecofit.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecofit.app.EcoFitApp
import com.ecofit.app.data.local.toModel
import com.ecofit.app.domain.TripSavings
import com.ecofit.app.ui.BigNumber
import com.ecofit.app.ui.Card
import com.ecofit.app.ui.Label
import com.ecofit.app.ui.ScreenTitle
import com.ecofit.app.ui.fmt
import com.ecofit.app.ui.won
import com.ecofit.app.ui.theme.EcoGreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel : ViewModel() {
    private val app = EcoFitApp.instance
    private val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000

    val weekly = app.db.tripDao().observeSince(weekAgo)
        .map { list -> list.map { it.toModel() } }
        .map { trips -> trips.size to app.savings.weekly(trips) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to app.savings.weekly(emptyList()))

    val snapshot = app.vehicleRepo.snapshot
}

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val (tripCount, s) by vm.weekly.collectAsStateWithLifecycle()
    val snap by vm.snapshot.collectAsStateWithLifecycle()

    Column {
        ScreenTitle("이번 주")
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Card(Modifier.weight(1.4f)) {
                Label("아낀 전기요금")
                Spacer(Modifier.height(8.dp))
                Text(s.savedWon.won(), fontSize = 64.sp, color = EcoGreen)
                Spacer(Modifier.height(8.dp))
                Label("커피 ${s.coffeeCups.fmt(1)}잔 · ${s.savedKm.fmt(0)}km 더 감 · 트립 ${tripCount}회")
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Card(Modifier.fillMaxWidth()) {
                    Label("회생으로 회수")
                    BigNumber(s.regenKwh.fmt(2), "kWh")
                }
                Card(Modifier.fillMaxWidth()) {
                    Label("평균 전비")
                    BigNumber(s.efficiencyKmPerKwh?.fmt(1) ?: "—", "km/kWh")
                    s.vsBaselinePercent?.let { Label("기준 대비 ${if (it >= 0) "+" else ""}$it%") }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth()) {
            Label("지금 차 상태")
            Spacer(Modifier.height(4.dp))
            Text(
                "배터리 ${snap.batterySocPercent?.fmt(0) ?: "—"}%  ·  남은 거리 ${snap.remainingRangeKm?.fmt(0) ?: "—"}km  ·  기어 ${snap.gear}  ·  외기 ${snap.outsideTempC?.fmt(0) ?: "—"}°C",
                fontSize = 20.sp,
            )
        }
    }
}
