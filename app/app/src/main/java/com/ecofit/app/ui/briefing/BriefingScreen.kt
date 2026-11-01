package com.ecofit.app.ui.briefing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecofit.app.EcoFitApp
import com.ecofit.app.ui.Card
import com.ecofit.app.ui.Label
import com.ecofit.app.ui.ScreenTitle
import com.ecofit.app.ui.fmt
import com.ecofit.app.ui.theme.EcoGreen
import com.ecofit.app.ui.theme.Warn

/**
 * 출발 전 브리핑.
 * 목적지까지 남은 거리는 NaviHelper(naviRepo)에서 받아온다. 사용자가 내비 앱에
 * 목적지를 설정해둔 경우에만 값이 들어오고, 없으면 null(안내 문구로 대체).
 */
@Composable
fun BriefingScreen() {
    val snap by EcoFitApp.instance.vehicleRepo.snapshot.collectAsStateWithLifecycle()
    val destKm by EcoFitApp.instance.naviRepo.remainDistanceKm.collectAsStateWithLifecycle()

    val range = snap.remainingRangeKm ?: 0f
    val margin = destKm?.let { range - it }
    val outside = snap.outsideTempC
    val hvacOn = snap.hvacOn == true
    val seatHeat = (snap.driverSeatHeatLevel ?: 0) > 0

    val tips = buildList {
        if (outside != null && outside < 12f && hvacOn && !seatHeat)
            add("히터 대신 시트 열선을 켜면 약 +8km 더 갈 수 있어요")
        if (margin != null && margin < 30f)
            add("여유가 ${margin.fmt(0)}km뿐이에요. 경로에 충전소를 넣을까요?")
        if (snap.driveMode == com.ecofit.app.data.model.DriveMode.SPORT)
            add("스포츠 모드는 전비가 약 15% 떨어져요. 에코로 바꿔볼까요?")
        if (isEmpty()) add("오늘 설정 좋아요. 그대로 출발하세요")
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ScreenTitle("출발 전")
        Card(Modifier.fillMaxWidth()) {
            Label("오늘 경로")
            Spacer(Modifier.height(8.dp))
            Text(
                destKm?.let { d -> "목적지 ${d.fmt(0)}km · 남은 거리 ${range.fmt(0)}km · 여유 ${(range - d).fmt(0)}km" }
                    ?: "설정된 목적지가 없어요. 내비게이션에서 목적지를 입력해보세요",
                fontSize = 28.sp,
                color = if (margin == null || margin >= 30f) EcoGreen else Warn,
            )
        }
        Card(Modifier.fillMaxWidth()) {
            Label("지금 상태")
            Spacer(Modifier.height(8.dp))
            Text(
                "배터리 ${snap.batterySocPercent?.fmt(0) ?: "—"}% · 외기 ${outside?.fmt(0) ?: "—"}°C · 공조 ${if (hvacOn) "${snap.hvacTargetTempC?.fmt(0)}°C" else "꺼짐"} · 열선 ${if (seatHeat) "켜짐" else "꺼짐"}",
                fontSize = 22.sp,
            )
        }
        Card(Modifier.fillMaxWidth()) {
            Label("제안")
            Spacer(Modifier.height(8.dp))
            tips.forEach { Text("• $it", fontSize = 24.sp) }
        }
    }
}
