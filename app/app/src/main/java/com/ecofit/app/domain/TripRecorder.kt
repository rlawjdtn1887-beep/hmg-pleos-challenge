package com.ecofit.app.domain

import com.ecofit.app.data.model.Gear
import com.ecofit.app.data.model.TripData
import com.ecofit.app.data.model.VehicleSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveTrip(
    val active: Boolean = false,
    val distanceKm: Float = 0f,
    val durationSec: Long = 0,
    val regenKwh: Float = 0f,
    val usedKwh: Float = 0f,
    val hardBrakeCount: Int = 0,
    val hardAccelCount: Int = 0,
    val hardBraking: Boolean = false,
    val hardAccelerating: Boolean = false,
)

// 시작: P/N -> D/R, 종료: D/R -> P. 최소 거리 미만이면 버린다.
// 회생은 차속이 있으면 RegenEstimator, 없으면 배터리 kWh 증가분으로 계산.
// 소모는 주행 중엔 차가 주는 전비(km/kWh)로 거리마다 쌓고, 주행이 끝나면 SoC 차이로 확정한다.
// 급제동/급가속은 1초 구간 가감속도로 판단. SDK 브레이크 값은 밟음 여부(0/100)뿐이라 쓰지 않는다.
class TripRecorder(
    private val estimator: RegenEstimator = RegenEstimator(),
    private val minTripKm: Float = 0.3f,
    private val hardBrakeDecel: Float = 4.0f,
    private val hardAccelMps2: Float = 3.0f,
    // 데모 대본의 가속 구간이 3.0 근처라 절반쯤만 걸려서, 데모에서는 더 낮은 값으로 확실히 잡는다
    private val demoHardAccelMps2: Float = 2.0f,
    private val windowMs: Long = 1000,
    private val eventGapMs: Long = 3000,
) {
    private var prev: VehicleSnapshot? = null
    private var acc: Accumulator? = null

    private val _live = MutableStateFlow(LiveTrip())
    val live: StateFlow<LiveTrip> = _live.asStateFlow()

    private class Accumulator(start: VehicleSnapshot) {
        val startTimeMs = start.timestampMs
        val startSoc = start.batterySocPercent
        val startOdo = start.odometerKm
        var speedDistKm = 0f
        var used = 0f
        var effUsed = 0f
        var regen = 0f
        var hardBrake = 0
        var hardAccel = 0
        var hvacSec = 0f
        var seatHeatSec = 0f
        var tempSum = 0f
        var tempCount = 0
        var winTimeMs: Long? = null
        var winSpeed = 0f
        var lastBrakeEventMs = Long.MIN_VALUE / 2
        var lastAccelEventMs = Long.MIN_VALUE / 2
    }

    fun feed(s: VehicleSnapshot, isDemo: Boolean = false): TripData? {
        val p = prev
        prev = s
        if (p == null) return null

        val wasDriving = p.gear == Gear.D || p.gear == Gear.R
        val isDriving = s.gear == Gear.D || s.gear == Gear.R

        if (!wasDriving && isDriving) {
            acc = Accumulator(s)
            _live.value = LiveTrip(active = true)
            return null
        }

        val a = acc ?: return null
        val now = s.timestampMs
        val dt = ((now - p.timestampMs) / 1000f).coerceIn(0.05f, 10f)

        val v0 = p.speedKmh
        val v1 = s.speedKmh
        if (v0 != null && v1 != null) {
            a.regen += estimator.step(v0, v1, dt)
            val segKm = (v0 + v1) / 2f / 3600f * dt
            a.speedDistKm += segKm
            // 전비가 들어오면 이번 구간 거리만큼 소모를 쌓는다. 1초마다 부드럽게 올라간다
            val eff = s.evEfficiencyKmPerKwh
            if (eff != null && eff in 1f..15f) a.effUsed += segKm / eff
        }

        val e0 = p.batteryEnergyKwh
        val e1 = s.batteryEnergyKwh
        if (e0 != null && e1 != null) {
            val d = e0 - e1
            if (d > 0f) a.used += d else if (v0 == null || v1 == null) a.regen += -d
        }

        // 1초마다 가감속도를 보고 이벤트는 3초에 한 번만 센다
        if (v1 != null) {
            val wt = a.winTimeMs
            if (wt == null) {
                a.winTimeMs = now; a.winSpeed = v1
            } else if (now - wt >= windowMs) {
                val accel = estimator.accel(a.winSpeed, v1, (now - wt) / 1000f)
                if (-accel >= hardBrakeDecel && now - a.lastBrakeEventMs >= eventGapMs) {
                    a.hardBrake++; a.lastBrakeEventMs = now
                }
                val accelThreshold = if (isDemo) demoHardAccelMps2 else hardAccelMps2
                if (accel >= accelThreshold && now - a.lastAccelEventMs >= eventGapMs) {
                    a.hardAccel++; a.lastAccelEventMs = now
                }
                a.winTimeMs = now; a.winSpeed = v1
            }
        }

        if (s.hvacOn == true) a.hvacSec += dt
        if ((s.driverSeatHeatLevel ?: 0) > 0) a.seatHeatSec += dt
        s.outsideTempC?.let { a.tempSum += it; a.tempCount++ }

        val dist = distance(a, s)
        _live.value = LiveTrip(
            active = true,
            distanceKm = dist,
            durationSec = (now - a.startTimeMs) / 1000,
            regenKwh = a.regen,
            usedKwh = if (a.used > 0f) a.used else a.effUsed,
            hardBrakeCount = a.hardBrake,
            hardAccelCount = a.hardAccel,
            hardBraking = now - a.lastBrakeEventMs < 2000,
            hardAccelerating = now - a.lastAccelEventMs < 2000,
        )

        if (wasDriving && !isDriving) {
            acc = null
            _live.value = _live.value.copy(active = false, hardBraking = false, hardAccelerating = false)
            if (dist < minTripKm) return null

            // 소모량 확정 순서: 배터리 kWh 차이 > SoC 차이 x 용량 > 전비로 쌓은 값
            // 차 전비가 회생을 뺀 순소비 기준이라 SoC 쪽도 회생을 더하지 않고 순소비로 맞춘다
            var used = a.used
            val cap = s.batteryCapacityKwh
            val endSoc = s.batterySocPercent
            if (used == 0f && a.startSoc != null && endSoc != null && cap != null && a.startSoc - endSoc >= 1f) {
                used = ((a.startSoc - endSoc) / 100f * cap).coerceAtLeast(0f)
            }
            if (used == 0f) used = a.effUsed

            return TripData(
                startTimeMs = a.startTimeMs,
                endTimeMs = now,
                distanceKm = dist,
                startSocPercent = a.startSoc,
                endSocPercent = s.batterySocPercent,
                energyUsedKwh = used,
                energyRegenKwh = a.regen,
                hardBrakeCount = a.hardBrake,
                hardAccelCount = a.hardAccel,
                hvacOnSec = a.hvacSec.toLong(),
                seatHeatOnSec = a.seatHeatSec.toLong(),
                avgOutsideTempC = if (a.tempCount > 0) a.tempSum / a.tempCount else null,
            )
        }
        return null
    }

    // 주행거리계가 움직이면 그걸, 아니면 차속 적분값
    private fun distance(a: Accumulator, s: VehicleSnapshot): Float {
        val start = a.startOdo
        val now = s.odometerKm
        val odo = if (start != null && now != null) (now - start).coerceAtLeast(0f) else 0f
        return if (odo > 0.01f) odo else a.speedDistKm
    }

    fun isRecording() = acc != null
}
