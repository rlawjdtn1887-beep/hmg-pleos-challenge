package com.ecofit.app.ui.drive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecofit.app.EcoFitApp
import com.ecofit.app.data.model.Gear
import com.ecofit.app.ui.Card
import com.ecofit.app.ui.Label
import com.ecofit.app.ui.ScreenTitle
import com.ecofit.app.ui.fmt
import com.ecofit.app.ui.theme.EcoGreen
import com.ecofit.app.ui.theme.EcoGreenDim
import com.ecofit.app.ui.theme.Slate
import com.ecofit.app.ui.theme.Warn

// 누적은 TripRecorder가 하고 이 화면은 보여주기만 한다
@Composable
fun DriveScreen() {
    val app = EcoFitApp.instance
    val snap by app.vehicleRepo.snapshot.collectAsStateWithLifecycle()
    val trip by app.recorder.live.collectAsStateWithLifecycle()

    val target = 0.3f
    val progress by animateFloatAsState(targetValue = (trip.regenKwh / target).coerceIn(0f, 1f), label = "regen")
    // brakePedalPercent는 SDK가 Boolean(밟음/안 밟음)만 줘서 0f/100f로 근사한
    // 값이라 "밟았는지"는 돼도 "세게 밟았는지"는 못 가린다. 급제동 여부는
    // TripRecorder가 감속도(실측 속도 기반)로 판단한 값(trip.brakingHard)을 쓴다.
    val pedal = snap.brakePedalPercent ?: 0f
    val braking = pedal > 10f
    val hardBrake = trip.brakingHard

    Column {
        ScreenTitle("주행 중")
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(300.dp)) {
                    val stroke = 28.dp.toPx()
                    val arc = Size(size.width - stroke, size.height - stroke)
                    val off = Offset(stroke / 2, stroke / 2)
                    drawArc(Slate, 135f, 270f, false, off, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(if (braking) EcoGreen else EcoGreenDim, 135f, 270f * progress, false, off, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text((trip.regenKwh * 1000).fmt(0), fontSize = 64.sp, color = EcoGreen)
                    Label("Wh 회수")
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(Modifier.fillMaxWidth()) {
                    Label("이번 트립")
                    Spacer(Modifier.height(4.dp))
                    Text("${trip.distanceKm.fmt(1)} km · ${trip.durationSec / 60}분 · ${snap.speedKmh?.fmt(0) ?: "—"} km/h", fontSize = 26.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("소모 ${(trip.usedKwh * 1000).fmt(0)} Wh · 회수 ${(trip.regenKwh * 1000).fmt(0)} Wh · 급제동 ${trip.hardBrakeCount}회", fontSize = 20.sp)
                }
                Card(Modifier.fillMaxWidth()) {
                    Label("코칭")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            snap.gear != Gear.D -> "출발하면 게이지가 차오릅니다"
                            hardBrake -> "급제동이에요. 조금 더 일찍 떼보세요"
                            braking -> "회생 중. 페달을 부드럽게 유지하세요"
                            else -> "앞차와 거리 두고 가속 페달을 떼면 회생이 시작돼요"
                        },
                        fontSize = 22.sp,
                        color = if (hardBrake) Warn else EcoGreen,
                    )
                }
            }
        }
    }
}
