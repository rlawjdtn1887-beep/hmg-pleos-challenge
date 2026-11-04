package com.ecofit.app.data.vehicle

import com.ecofit.app.data.model.DriveMode
import com.ecofit.app.data.model.Gear
import com.ecofit.app.data.model.VehicleSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * 가짜 주행 시뮬레이터.
 * 출발 → 가속/정속/감속 반복 → 도착 시나리오를 1초 간격으로 흘려보낸다.
 * SDK 없이 홈/리포트/게이지 화면을 만들 때 쓴다.
 */
class FakeVehicleRepository(
    private val cycleSec: Int = 90,
) : VehicleRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _snapshot = MutableStateFlow(
        VehicleSnapshot(
            batterySocPercent = 78f,
            batteryEnergyKwh = 60.4f,
            batteryCapacityKwh = 77.4f,
            remainingRangeKm = 310f,
            evEfficiencyKmPerKwh = 5.1f,
            isCharging = false,
            brakePedalPercent = 0f,
            gear = Gear.P,
            driveMode = DriveMode.NORMAL,
            hvacOn = true,
            hvacTargetTempC = 22f,
            outsideTempC = 8f,
            fanSpeed = 3,
            driverSeatHeatLevel = 0,
            passengerSeatHeatLevel = 2,
            odometerKm = 12_430f,
            tripDistanceKm = 0f,
            tripDurationSec = 0,
        )
    )
    override val snapshot: StateFlow<VehicleSnapshot> = _snapshot.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override fun start() {
        if (job?.isActive == true) return
        _isConnected.value = true
        job = scope.launch {
            var t = 0
            var speedKmh = 0f
            while (true) {
                delay(1000)
                t++
                val phase = t % cycleSec

                val (gear, targetSpeed, brake) = when {
                    phase < 5 -> Triple(Gear.P, 0f, 0f)
                    phase < 25 -> Triple(Gear.D, 60f, 0f)
                    phase < 45 -> Triple(Gear.D, 80f, 0f)
                    phase < 55 -> Triple(Gear.D, 30f, Random.nextFloat() * 60f + 20f)
                    phase < 75 -> Triple(Gear.D, 70f, 0f)
                    phase < 85 -> Triple(Gear.D, 0f, Random.nextFloat() * 40f + 40f)
                    else -> Triple(Gear.P, 0f, 0f)
                }

                speedKmh += (targetSpeed - speedKmh) * 0.3f
                val distDeltaKm = speedKmh / 3600f

                // 가속·정속은 소모, 감속(브레이크)은 회생
                val energyDeltaKwh = when {
                    brake > 10f -> -0.004f * (brake / 100f)
                    speedKmh > 1f -> 0.0055f * (speedKmh / 80f)
                    else -> 0.0002f
                }

                _snapshot.update { s ->
                    val energy = (s.batteryEnergyKwh ?: 60f) - energyDeltaKwh
                    val cap = s.batteryCapacityKwh ?: 77.4f
                    val soc = max(0f, min(100f, energy / cap * 100f))
                    val inTrip = gear == Gear.D
                    s.copy(
                        timestampMs = System.currentTimeMillis(),
                        batteryEnergyKwh = energy,
                        batterySocPercent = soc,
                        remainingRangeKm = soc / 100f * 400f,
                        brakePedalPercent = brake,
                        speedKmh = speedKmh,
                        gear = gear,
                        odometerKm = (s.odometerKm ?: 0f) + distDeltaKm,
                        tripDistanceKm = if (inTrip) (s.tripDistanceKm ?: 0f) + distDeltaKm else if (gear == Gear.P && phase < 5) 0f else s.tripDistanceKm,
                        tripDurationSec = if (inTrip) (s.tripDurationSec ?: 0L) + 1 else s.tripDurationSec,
                    )
                }
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        _isConnected.value = false
    }
}
