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

// 심사용 데모 주행. 정해진 대본대로 한 번 달리고 P로 끝난다.
// 가속, 긴 회생, 급제동 한 번, 정차까지 2분 안에 전부 나온다.
class DemoVehicleRepository(
    private val onFinished: () -> Unit,
) : VehicleRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _snapshot = MutableStateFlow(
        VehicleSnapshot(
            batterySocPercent = 72f,
            batteryEnergyKwh = 55.7f,
            batteryCapacityKwh = 77.4f,
            remainingRangeKm = 288f,
            isCharging = false,
            brakePedalPercent = 0f,
            speedKmh = 0f,
            gear = Gear.P,
            driveMode = DriveMode.ECO,
            hvacOn = true,
            hvacTargetTempC = 22f,
            outsideTempC = 14f,
            fanSpeed = 2,
            driverSeatHeatLevel = 0,
        )
    )
    override val snapshot: StateFlow<VehicleSnapshot> = _snapshot.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // (구간 길이 초, 목표 속도, 브레이크) 순서. 속도는 구간 안에서 선형으로 바뀐다.
    private val script = listOf(
        Step(3, 0f, 0f, Gear.P),
        Step(2, 0f, 0f, Gear.D),
        Step(10, 50f, 0f, Gear.D),
        Step(8, 50f, 0f, Gear.D),
        Step(12, 15f, 40f, Gear.D),
        Step(10, 70f, 0f, Gear.D),
        Step(8, 70f, 0f, Gear.D),
        Step(3, 20f, 100f, Gear.D),
        Step(4, 20f, 0f, Gear.D),
        Step(10, 65f, 0f, Gear.D),
        Step(14, 10f, 35f, Gear.D),
        Step(8, 45f, 0f, Gear.D),
        Step(12, 0f, 30f, Gear.D),
        Step(3, 0f, 0f, Gear.D),
        Step(3, 0f, 0f, Gear.P),
    )

    private class Step(val sec: Int, val targetKmh: Float, val brake: Float, val gear: Gear)

    override fun start() {
        if (job?.isActive == true) return
        _isConnected.value = true
        job = scope.launch {
            var speed = 0f
            for (step in script) {
                val from = speed
                val ticks = step.sec * TICKS_PER_SEC
                for (i in 1..ticks) {
                    delay(1000L / TICKS_PER_SEC)
                    speed = from + (step.targetKmh - from) * i / ticks
                    val dtH = 1f / TICKS_PER_SEC / 3600f
                    _snapshot.update { s ->
                        s.copy(
                            timestampMs = System.currentTimeMillis(),
                            speedKmh = speed,
                            gear = step.gear,
                            brakePedalPercent = step.brake,
                            odometerKm = (s.odometerKm ?: 0f) + speed * dtH,
                        )
                    }
                }
            }
            _isConnected.value = false
            onFinished()
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        _isConnected.value = false
    }

    private companion object {
        const val TICKS_PER_SEC = 4
    }
}
