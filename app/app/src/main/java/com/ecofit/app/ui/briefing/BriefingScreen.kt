package com.ecofit.app.ui.briefing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecofit.app.EcoFitApp
import com.ecofit.app.data.model.DriveMode
import com.ecofit.app.ui.Amount
import com.ecofit.app.ui.Tile
import com.ecofit.app.ui.TileLabel
import com.ecofit.app.ui.TileTone
import com.ecofit.app.ui.fmt
import com.ecofit.app.ui.theme.Amber
import com.ecofit.app.ui.theme.Ice
import com.ecofit.app.ui.theme.InkMuted

private const val LOW_MARGIN_KM = 30f

@Composable
fun BriefingScreen() {
    val app = EcoFitApp.instance
    val snap by app.vehicleRepo.snapshot.collectAsStateWithLifecycle()
    val destKm by app.naviRepo.remainDistanceKm.collectAsStateWithLifecycle()
    val etaMin by app.naviRepo.remainTimeMin.collectAsStateWithLifecycle()

    // 0은 값을 못 받은 경우로 본다
    val range = snap.remainingRangeKm?.takeIf { it > 0f }
    val dest = destKm?.takeIf { it > 0f }
    val margin = if (range != null && dest != null) range - dest else null
    val low = margin != null && margin < LOW_MARGIN_KM

    val outside = snap.outsideTempC
    val hvacOn = snap.hvacOn == true
    val seatHeat = (snap.driverSeatHeatLevel ?: 0) > 0

    val tips = buildList {
        if (outside != null && outside < 12f && hvacOn && !seatHeat)
            add("히터 대신 시트 열선을 켜면 약 8km 더 갈 수 있어요")
        if (margin != null && margin < 0f)
            add("이대로는 ${(-margin).fmt(0)}km 모자라요. 경로에 충전소를 넣어주세요")
        else if (low)
            add("여유가 ${margin!!.fmt(0)}km뿐이에요. 충전소를 경로에 넣어보세요")
        if (snap.driveMode == DriveMode.SPORT)
            add("스포츠 모드는 전비가 약 15% 떨어져요. 에코 모드를 추천해요")
        if (isEmpty()) add("지금 설정 좋아요. 그대로 출발하세요")
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile(if (low) TileTone.Dark else TileTone.Accent, Modifier.weight(2f).fillMaxHeight()) {
                when {
                    dest == null -> {
                        TileLabel("오늘 경로")
                        Text(
                            "내비에 목적지를 입력하면\n배터리가 충분한지 알려드려요",
                            fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 58.sp,
                        )
                    }
                    margin == null -> {
                        TileLabel("목적지까지")
                        Column {
                            Amount(dest.fmt(0), "km", 150.sp)
                            Text("남은 주행거리 확인 중", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    else -> {
                        TileLabel(
                            if (low) "충전이 필요할 수 있어요" else "목적지에 도착해도 남는 거리",
                            color = if (low) Amber else InkMuted,
                        )
                        Column {
                            Amount(margin.fmt(0), "km", 150.sp)
                            Text(
                                "목적지 ${dest.fmt(0)}km" + (etaMin?.let { " · 도착 약 ${it}분 후" } ?: ""),
                                fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Tile(TileTone.Light, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel("남은 주행거리")
                Column {
                    Amount(range?.fmt(0) ?: "—", "km", 96.sp)
                    Text(
                        "배터리 ${snap.batterySocPercent?.fmt(0) ?: "—"}%",
                        fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = InkMuted,
                    )
                }
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile(TileTone.Dark, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel("지금 상태")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("외기 ${outside?.fmt(0) ?: "—"}°C", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "공조 " + if (hvacOn) "${snap.hvacTargetTempC?.fmt(0) ?: "—"}°C" else "꺼짐",
                        fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    )
                    Text("열선 " + if (seatHeat) "켜짐" else "꺼짐", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tile(TileTone.Dark, Modifier.weight(2f).fillMaxHeight()) {
                TileLabel("출발 전 제안", color = Ice)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tips.forEach {
                        Text(it, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 46.sp)
                    }
                }
            }
        }
    }
}
