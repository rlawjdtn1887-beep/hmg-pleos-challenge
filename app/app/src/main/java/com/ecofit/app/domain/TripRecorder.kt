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
    val brakingHard: Boolean = false,
)

// 시작: P/N -> D/R, 종료: D/R -> P. 최소 거리 미만이면 버린다.
// 회생은 차속이 있으면 RegenEstimator, 없으면 배터리 kWh 증가분으로 계산.
class TripRecorder(
    private val estimator: RegenEstimator = RegenEstimator(),
    private val minTripKm: Float = 0.3f,
    private val hardBrakeDecel: Float = 3.0f,
    private val hardAccelMps2: Float = 2.5f,
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
        var regen = 0f
        var hardBrake = 0
        var hardAccel = 0
        var hvacSec = 0f
        var seatHeatSec = 0f
        var tempSum = 0f
        var tempCount = 0
        var brakingHard = false
        var accelHard = false
    }

    fun feed(s: VehicleSnapshot): TripData? {
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
        val dt = ((s.timestampMs - p.timestampMs) / 1000f).coerceIn(0.05f, 10f)

        val v0 = p.speedKmh
        val v1 = s.speedKmh
        var accelMps2: Float? = null
        if (v0 != null && v1 != null) {
            a.regen += estimator.step(v0, v1, dt)
            a.speedDistKm += (v0 + v1) / 2f / 3600f * dt
            accelMps2 = estimator.accel(v0, v1, dt)
        }

        val e0 = p.batteryEnergyKwh
        val e1 = s.batteryEnergyKwh
        if (e0 != null && e1 != null) {
            val d = e0 - e1
            if (d > 0f) a.used += d else if (v0 == null || v1 == null) a.regen += -d
        }

        // brakePedalPercent는 SDK가 Boolean(밟음/안 밟음)만 줘서 0f/100f로 근사한
        // 값이라 "세게 밟았는지"를 못 가린다 (살짝만 밟아도 100f). 감속도(실측 속도
        // 기반)만으로 급제동을 판단한다.
        val hardBrakeNow = accelMps2 != null && -accelMps2 >= hardBrakeDecel
        if (hardBrakeNow && !a.brakingHard) a.hardBrake++
        a.brakingHard = hardBrakeNow

        val hardAccelNow = accelMps2 != null && accelMps2 >= hardAccelMps2
        if (hardAccelNow && !a.accelHard) a.hardAccel++
        a.accelHard = hardAccelNow

        if (s.hvacOn == true) a.hvacSec += dt
        if ((s.driverSeatHeatLevel ?: 0) > 0) a.seatHeatSec += dt
        s.outsideTempC?.let { a.tempSum += it; a.tempCount++ }

        val dist = distance(a, s)
        _live.value = LiveTrip(
            active = true,
            distanceKm = dist,
            durationSec = (s.timestampMs - a.startTimeMs) / 1000,
            regenKwh = a.regen,
            usedKwh = a.used,
            hardBrakeCount = a.hardBrake,
            hardAccelCount = a.hardAccel,
            brakingHard = a.brakingHard,
        )

        if (wasDriving && !isDriving) {
            acc = null
            _live.value = _live.value.copy(active = false)
            if (dist < minTripKm) return null

            // kWh를 못 받는 차는 SoC 차이로 소모량 계산
            var used = a.used
            val cap = s.batteryCapacityKwh
            if (used == 0f && a.startSoc != null && s.batterySocPercent != null && cap != null) {
                used = ((a.startSoc - s.batterySocPercent) / 100f * cap + a.regen).coerceAtLeast(0f)
            }

            return TripData(
                startTimeMs = a.startTimeMs,
                endTimeMs = s.timestampMs,
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

    // 주행거리계가 있으면 그걸, 없으면 차속 적분값
    private fun distance(a: Accumulator, s: VehicleSnapshot): Float {
        val start = a.startOdo
        val now = s.odometerKm
        return if (start != null && now != null) (now - start).coerceAtLeast(0f) else a.speedDistKm
    }

    fun isRecording() = acc != null
}
