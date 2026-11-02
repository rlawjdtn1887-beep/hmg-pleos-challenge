package com.ecofit.app.ui.drive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecofit.app.EcoFitApp
import com.ecofit.app.data.model.Gear
import com.ecofit.app.ui.Amount
import com.ecofit.app.ui.ProgressBar
import com.ecofit.app.ui.Tile
import com.ecofit.app.ui.TileLabel
import com.ecofit.app.ui.TileTone
import com.ecofit.app.ui.fmt
import com.ecofit.app.ui.theme.Amber
import com.ecofit.app.ui.theme.Ice
import com.ecofit.app.ui.theme.Ink
import com.ecofit.app.ui.theme.InkMuted
import com.ecofit.app.ui.theme.Paper
import kotlin.math.roundToInt

// SavingsCalculator 단가와 같게 유지
private const val WON_PER_KWH = 350f
private const val TARGET_KWH = 0.3f

// 누적은 TripRecorder가 하고 이 화면은 보여주기만 한다
@Composable
fun DriveScreen() {
    val app = EcoFitApp.instance
    val snap by app.vehicleRepo.snapshot.collectAsStateWithLifecycle()
    val trip by app.recorder.live.collectAsStateWithLifecycle()

    val won = (trip.regenKwh * WON_PER_KWH).roundToInt()
    val progress by animateFloatAsState((trip.regenKwh / TARGET_KWH).coerceIn(0f, 1f), label = "regen")
    val pedal = snap.brakePedalPercent ?: 0f
    val hardBrake = pedal > 60f
    val braking = pedal > 10f

    val coach = when {
        snap.gear != Gear.D -> "출발하면 회생 제동을 기록해요"
        hardBrake -> "급제동이에요\n조금 더 일찍 페달을 떼보세요"
        braking -> "회생 중이에요\n페달을 부드럽게 유지하세요"
        else -> "앞차와 거리를 두고\n가속 페달을 떼면 회생이 시작돼요"
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tile(TileTone.Accent, Modifier.weight(2f).fillMaxHeight()) {
                TileLabel("이번 주행에서 회생 제동으로 아낀 돈")
                Column {
                    Amount("%,d".format(won), "원", 150.sp)
                    Spacer(Modifier.height(16.dp))
                    ProgressBar(progress, track = Ink.copy(alpha = 0.15f), fill = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${(trip.regenKwh * 1000).fmt(0)}Wh 되찾음 · 목표 ${(TARGET_KWH * 1000).toInt()}Wh",
                        fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                    )
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
                Text(
                    coach,
                    fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 58.sp,
                    color = if (hardBrake) Amber else Paper,
                )
            }
            Tile(TileTone.Light, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel("이번 주행")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${trip.distanceKm.fmt(1)}km · ${trip.durationSec / 60}분", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("급제동 ${trip.hardBrakeCount}회", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("소모 ${(trip.usedKwh * 1000).fmt(0)}Wh", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = InkMuted)
                }
            }
        }
    }
}
