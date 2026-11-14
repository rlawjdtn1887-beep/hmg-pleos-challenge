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
// 가속, 긴 회생, 급제동 한 번, 저배터리 충전소 안내까지 약 50초, 이후 15초 정차 구간을 두고 P로 끝난다.
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
            // 전비를 넣어 둬야 소모량이 거리 기준으로 계산된다. 없으면 연출용 배터리 감소가 소모량으로 잡혀 숫자가 망가진다
            evEfficiencyKmPerKwh = DEMO_EFFICIENCY,
        )
    )
    override val snapshot: StateFlow<VehicleSnapshot> = _snapshot.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // (구간 길이 초, 목표 속도, 브레이크) 순서. 속도는 구간 안에서 선형으로 바뀐다.
    // 급가속은 맨 처음 0->50km/h 2초(약 7m/s²) 한 번만, 급제동은 65->10km/h 3초(약 5m/s²) 한 번만 걸리게 하고
    // 나머지 가속은 2.5m/s² 아래로 둔다. ±8% 변동이 붙어도 판정(가속 3.0 / 감속 4.0)이 뒤집히지 않는 값이다
    private val script = listOf(
        Step(2, 0f, 0f, Gear.P),
        Step(1, 0f, 0f, Gear.D),
        Step(2, 50f, 0f, Gear.D),
        Step(3, 50f, 0f, Gear.D),
        Step(7, 15f, 40f, Gear.D),
        Step(8, 65f, 0f, Gear.D),
        Step(2, 65f, 0f, Gear.D),
        Step(3, 10f, 100f, Gear.D),
        Step(7, 55f, 0f, Gear.D),
        Step(2, 55f, 0f, Gear.D),
        Step(6, 5f, 35f, Gear.D),
        Step(2, 0f, 30f, Gear.D),
        Step(1, 0f, 0f, Gear.D),
    )

    // 저배터리로 충전소 안내가 뜬 뒤 "출발 전" 화면에서 바뀐 목적지(충전소까지 거리)까지
    // 둘러볼 시간을 주는 정차 구간. SOC 진행률 계산(REFERENCE)에는 안 넣어서, 이 구간 내내
    // 배터리는 20% 아래로 고정돼 있다. 이 구간이 끝나면 P로 세워 데모를 마친다
    private val bufferStep = Step(BUFFER_SEC, 0f, 0f, Gear.D)
    private val endStep = Step(2, 0f, 0f, Gear.P)

    private class Step(val sec: Int, val targetKmh: Float, val brake: Float, val gear: Gear) {
        // 매번 똑같이 재생되면 리포트에 똑같은 숫자만 쌓여서 지난 주행 비교가 의미 없어진다.
        // 구간 길이·목표 속도를 ±8% 흔들어서 실행마다 거리·회생량이 달라지게 한다.
        // 더 크게 흔들면 급가속·급제동 판정이 실행마다 바뀌어 점수가 들쭉날쭉해진다
        fun jittered(): Step {
            val speedF = Random.nextFloat() * 0.16f + 0.92f
            val secF = Random.nextFloat() * 0.16f + 0.92f
            return Step((sec * secF).roundToInt().coerceAtLeast(1), targetKmh * speedF, brake, gear)
        }
    }

    override fun start() {
        if (job?.isActive == true) return
        _isConnected.value = true
        val runScript = script.map { it.jittered() }
        // SOC는 bufferStep·endStep을 뺀 "원래 대본" 기준으로만 72%->14%로 줄어들고,
        // 그 뒤 정차 구간에서는 14%에 고정된 채로 유지된다 (progress가 1을 넘어도 coerceAtMost로 묶음)
        val referenceTicks = runScript.sumOf { it.sec * TICKS_PER_SEC }
        val totalTicks = referenceTicks + (bufferStep.sec + endStep.sec) * TICKS_PER_SEC
        job = scope.launch {
            var speed = 0f
            var elapsedTicks = 0
            for (step in runScript + bufferStep + endStep) {
                val from = speed
                val ticks = step.sec * TICKS_PER_SEC
                for (i in 1..ticks) {
                    delay(1000L / TICKS_PER_SEC)
                    elapsedTicks++
                    speed = from + (step.targetKmh - from) * i / ticks
                    val dtH = 1f / TICKS_PER_SEC / 3600f
                    // 배터리를 끝까지 72%로 고정해 두면 저배터리 알림·충전소 안내를 데모로 보여줄 수가 없어서,
                    // 대본 진행률에 맞춰 72% -> 14%로 서서히 줄어들게 한다 (거의 끝날 때쯤 20% 아래로 떨어짐)
                    val progress = (elapsedTicks.toFloat() / referenceTicks).coerceAtMost(1f)
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
        // 저배터리 안내 뜬 뒤 화면을 둘러볼 수 있게 주는 정차 시간(초)
        const val BUFFER_SEC = 15
        const val START_SOC = 72f
        const val END_SOC = 14f
        const val START_RANGE_KM = 288f
        const val START_ENERGY_KWH = 55.7f
        // 리포트 전비 비교·에코 점수 보너스에 쓰인다. 기준(4.5)보다 조금 좋은 값
        const val DEMO_EFFICIENCY = 5.0f
    }
}
