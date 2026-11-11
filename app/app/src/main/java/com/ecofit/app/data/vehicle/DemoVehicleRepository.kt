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
import kotlin.math.roundToInt
import kotlin.random.Random

// 심사용 데모 주행. 정해진 대본대로 한 번 달리고 P로 끝난다.
// 가속, 긴 회생, 급제동 한 번, 정차까지 약 45초 안에 전부 나온다.
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
            outsideTempC = 8f,
            fanSpeed = 2,
            driverSeatHeatLevel = 0,
            passengerSeatHeatLevel = 0,
        )
    )
    override val snapshot: StateFlow<VehicleSnapshot> = _snapshot.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // (구간 길이 초, 목표 속도, 브레이크) 순서. 속도는 구간 안에서 선형으로 바뀐다.
    private val script = listOf(
        Step(2, 0f, 0f, Gear.P),
        Step(1, 0f, 0f, Gear.D),
        Step(5, 55f, 0f, Gear.D),
        Step(3, 55f, 0f, Gear.D),
        Step(7, 15f, 40f, Gear.D),
        Step(5, 70f, 0f, Gear.D),
        Step(2, 70f, 0f, Gear.D),
        Step(3, 20f, 100f, Gear.D),
        Step(2, 20f, 0f, Gear.D),
        Step(4, 60f, 0f, Gear.D),
        Step(6, 5f, 35f, Gear.D),
        Step(3, 0f, 30f, Gear.D),
        Step(1, 0f, 0f, Gear.D),
        Step(2, 0f, 0f, Gear.P),
    )

    private class Step(val sec: Int, val targetKmh: Float, val brake: Float, val gear: Gear) {
        // 매번 똑같이 재생되면 리포트에 똑같은 숫자만 쌓여서 지난 주행 비교가 의미 없어진다.
        // 구간 길이·목표 속도를 ±15% 흔들어서 실행마다 거리·급제동 여부·회생량이 달라지게 한다
        fun jittered(): Step {
            val speedF = Random.nextFloat() * 0.3f + 0.85f
            val secF = Random.nextFloat() * 0.3f + 0.85f
            return Step((sec * secF).roundToInt().coerceAtLeast(1), targetKmh * speedF, brake, gear)
        }
    }

    override fun start() {
        if (job?.isActive == true) return
        _isConnected.value = true
        val runScript = script.map { it.jittered() }
        val totalTicks = runScript.sumOf { it.sec * TICKS_PER_SEC }
        job = scope.launch {
            var speed = 0f
            var elapsedTicks = 0
            for (step in runScript) {
                val from = speed
                val ticks = step.sec * TICKS_PER_SEC
                for (i in 1..ticks) {
                    delay(1000L / TICKS_PER_SEC)
                    elapsedTicks++
                    speed = from + (step.targetKmh - from) * i / ticks
                    val dtH = 1f / TICKS_PER_SEC / 3600f
                    // 배터리를 끝까지 72%로 고정해 두면 저배터리 알림·충전소 안내를 데모로 보여줄 수가 없어서,
                    // 대본 진행률에 맞춰 72% -> 14%로 서서히 줄어들게 한다 (거의 끝날 때쯤 20% 아래로 떨어짐)
                    val progress = elapsedTicks.toFloat() / totalTicks
                    val soc = START_SOC - progress * (START_SOC - END_SOC)
                    _snapshot.update { s ->
                        s.copy(
                            timestampMs = System.currentTimeMillis(),
                            speedKmh = speed,
                            gear = step.gear,
                            brakePedalPercent = step.brake,
                            odometerKm = (s.odometerKm ?: 0f) + speed * dtH,
                            batterySocPercent = soc,
                            remainingRangeKm = START_RANGE_KM * soc / START_SOC,
                            batteryEnergyKwh = START_ENERGY_KWH * soc / START_SOC,
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
        const val START_SOC = 72f
        const val END_SOC = 14f
        const val START_RANGE_KM = 288f
        const val START_ENERGY_KWH = 55.7f
    }
}
