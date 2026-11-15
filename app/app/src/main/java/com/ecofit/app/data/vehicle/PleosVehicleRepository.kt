package com.ecofit.app.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.control.DrivingModeType
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.DrivingModeStateListener
import ai.pleos.playground.vehicle.listener.DrivingTimeListener
import ai.pleos.playground.vehicle.listener.EvBatteryEfficiencyListener
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.EvChargePortOpenListener
import ai.pleos.playground.vehicle.listener.HvacFanSpeedListener
import ai.pleos.playground.vehicle.listener.HvacPowerStateListener
import ai.pleos.playground.vehicle.listener.HvacSeatTemperatureListener
import ai.pleos.playground.vehicle.listener.HvacTemperatureValueListener
import ai.pleos.playground.vehicle.listener.RangeRemainingListener
import android.content.Context
import android.util.Log
import com.ecofit.app.data.model.DriveMode
import com.ecofit.app.data.model.VehicleSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Pleos Vehicle SDK 연동.
 *
 * 구조:
 *   start()  -> vehicle.initialize() -> 각 도메인 capability 확인 -> register 콜백
 *   콜백     -> _snapshot.update { copy(...) }
 *   stop()   -> unregister -> vehicle.release()
 *
 * 각 도메인의 정확한 메서드/리스너 이름은 Android Studio에서
 * vehicle.getEvBattery(). 까지 치고 자동완성으로 확인.
 * 샘플: https://github.com/Pleos-Playground/pleos-sample-vehicle-app (VehicleClient.kt 의 Door 패턴과 동일)
 *
 * Vehicle 2.3.2 SDK(aar) 실제 디컴파일 결과로 확인한 사항:
 *  - remainingRangeKm 은 EvBattery 가 아니라 Odometer.registerRangeRemainingListener 에 있음
 *  - Brake 는 페달 위치(%) API가 없고 getCurrentBrakePedalStatus/registerBrakePedalStatus 가
 *    Boolean(밟음/안 밟음)만 줌 -> brakePedalPercent 는 0f/100f 로만 근사 가능
 *  - batteryEnergyKwh(순간 잔존 kWh), HVAC 의 outsideTempC 에 대응하는 API가
 *    SDK에 없음 -> null 유지 (차종/에뮬레이터가 늘려주면 그때 다시 확인)
 *  - isCharging: getEvChargeSwitchState()가 의미상 더 정확하지만 콜백 한 번뿐이라 실시간
 *    반영이 안 됨. 대신 registerEvChargePortOpen(충전 포트 열림)을 proxy로 써서 실시간으로 받는다
 *  - vehicle.getHvac() 아님, vehicle.getHVAC() (SDK 클래스명이 전부 대문자)
 */
class PleosVehicleRepository(context: Context) : VehicleRepository {

    private val tag = "PleosVehicleRepo"
    private val vehicle = Vehicle(context.applicationContext)
    private var initialized = false

    // 운전석 = Row1 Left (국내는 좌핸들 기준), 조수석 = Row1 Right
    private val driverSeat = SeatArea(SeatValue.ROW_1_LEFT)
    private val passengerSeat = SeatArea(SeatValue.ROW_1_RIGHT)

    private val _snapshot = MutableStateFlow(VehicleSnapshot())
    override val snapshot: StateFlow<VehicleSnapshot> = _snapshot.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val carProps = CarPropertySource(
        context,
        onSpeedKmh = { v -> _snapshot.update { it.copy(speedKmh = v, timestampMs = now()) } },
        onGear = { g -> _snapshot.update { it.copy(gear = g, timestampMs = now()) } },
        onOutsideTempC = { t -> _snapshot.update { it.copy(outsideTempC = t, timestampMs = now()) } },
    )

    // ---- 리스너. stop()에서 unregister 하려면 인스턴스를 들고 있어야 한다 ----

    private val evBatterySocListener = object : EvBatteryStateOfChargeListener {
        override fun onEvBatteryStateOfChargeUpdated(value: Int?) {
            _snapshot.update { it.copy(batterySocPercent = value?.toFloat(), timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("EvBatteryStateOfCharge", e)
    }

    private val evEfficiencyListener = object : EvBatteryEfficiencyListener {
        override fun onEvBatteryEfficiencyUpdated(value: Float?) {
            _snapshot.update { it.copy(evEfficiencyKmPerKwh = value, timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("CurrentEvEfficiency", e)
    }

    // 포트가 열려있다 = 충전 케이블이 꽂혀 충전 중이라고 본다 (isCharging 대응 API가 따로 없어 이걸로 근사)
    private val evChargePortOpenListener = object : EvChargePortOpenListener {
        override fun onEvChargePortOpenUpdated(value: Boolean?) {
            _snapshot.update { it.copy(isCharging = value, timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("EvChargePortOpen", e)
    }

    private val rangeRemainingListener = object : RangeRemainingListener {
        override fun onRangeRemainingChanged(value: Float?) {
            _snapshot.update { it.copy(remainingRangeKm =value?.div(1000f), timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("RangeRemaining", e)
    }

    private val brakePedalListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(value: Boolean?) {
            // SDK가 페달 위치(%)가 아니라 밟음 여부만 줘서 0f/100f로 근사
            _snapshot.update { it.copy(brakePedalPercent = if (value == true) 100f else 0f, timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("BrakePedalStatus", e)
    }

    private val drivingModeListener = object : DrivingModeStateListener {
        override fun onDrivingModeStateUpdated(value: DrivingModeType?) {
            val mode = when (value) {
                DrivingModeType.ECO -> DriveMode.ECO
                DrivingModeType.NORMAL -> DriveMode.NORMAL
                DrivingModeType.SPORT -> DriveMode.SPORT
                else -> DriveMode.UNKNOWN
            }
            _snapshot.update { it.copy(driveMode = mode, timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("DrivingModeState", e)
    }

    private val hvacPowerListener = object : HvacPowerStateListener {
        override fun onHvacPowerStateUpdated(value: Boolean?) {
            _snapshot.update { it.copy(hvacOn = value, timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("HvacPowerState", e)
    }

    private val hvacTemperatureListener = object : HvacTemperatureValueListener {
        override fun onHvacTemperatureUpdated(value: Float?, unit: Float?) {
            _snapshot.update { it.copy(hvacTargetTempC = value, timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("HvacTemperature", e)
    }

    private val hvacFanSpeedListener = object : HvacFanSpeedListener {
        override fun onHvacFanSpeedUpdated(value: Int?) {
            _snapshot.update { it.copy(fanSpeed = value, timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("HvacFanSpeed", e)
    }

    private val seatHeatListener = object : HvacSeatTemperatureListener {
        override fun onHvacSeatTemperatureUpdated(value: Int?) {
            _snapshot.update { it.copy(driverSeatHeatLevel = value, timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("HvacSeatTemperature", e)
    }

    private val passengerSeatHeatListener = object : HvacSeatTemperatureListener {
        override fun onHvacSeatTemperatureUpdated(value: Int?) {
            _snapshot.update { it.copy(passengerSeatHeatLevel = value, timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("PassengerHvacSeatTemperature", e)
    }

    private val odometerListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(value: Float?) {
            _snapshot.update { it.copy(odometerKm = value, timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("DistanceDriven", e)
    }

    private val tripDistanceListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(value: Float?) {
            _snapshot.update { it.copy(tripDistanceKm = value, timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("TripCurrentDrivingDistance", e)
    }

    private val tripDurationListener = object : DrivingTimeListener {
        override fun onDrivingTimeUpdated(value: Int?) {
            _snapshot.update { it.copy(tripDurationSec = value?.toLong(), timestampMs = now()) }
        }
        override fun onFailed(e: Exception) = logFailure("TripCurrentDrivingTime", e)
    }

    override fun start() {
        // 권한 허용 후 다시 불릴 수 있어서 initialized 체크 전에 둔다
        carProps.start()
        if (initialized) return
        try {
            vehicle.initialize()
            initialized = true
            _isConnected.value = true
        } catch (e: Exception) {
            Log.e(tag, "initialize failed", e)
            _isConnected.value = false
            return
        }

        registerEvBattery()
        registerBrake()
        registerDrivingMode()
        registerHvac()
        registerSeat()
        registerOdometer()
    }

    override fun stop() {
        carProps.stop()
        if (!initialized) return
        try {
            vehicle.getEvBattery().apply {
                unregisterEvBatteryStateOfCharge(evBatterySocListener)
                unregisterCurrentEvEfficiency(evEfficiencyListener)
                unregisterEvChargePortOpen(evChargePortOpenListener)
            }
            vehicle.getBrake().unregisterBrakePedalStatus(brakePedalListener)
            vehicle.getDrivingMode().unregisterDrivingModeState(drivingModeListener)
            vehicle.getHVAC().apply {
                unregisterHvacPowerState(driverSeat, hvacPowerListener)
                unregisterHvacTemperature(driverSeat, hvacTemperatureListener)
                unregisterHvacFanSpeed(driverSeat, hvacFanSpeedListener)
            }
            vehicle.getSeat().apply {
                unregisterHvacSeatTemperature(driverSeat, seatHeatListener)
                unregisterHvacSeatTemperature(passengerSeat, passengerSeatHeatListener)
            }
            vehicle.getOdometer().apply {
                unregisterDistanceDriven(odometerListener)
                unregisterTripCurrentDrivingDistance(tripDistanceListener)
                unregisterTripCurrentDrivingTime(tripDurationListener)
                unregisterRangeRemainingListener(rangeRemainingListener)
            }
            vehicle.release()
        } catch (e: Exception) {
            Log.e(tag, "release failed", e)
        }
        initialized = false
        _isConnected.value = false
    }

    // ---- 도메인별 등록. 패턴은 전부 같다: register(listener) ----

    private fun registerEvBattery() {
        val ev = vehicle.getEvBattery()
        ev.registerEvBatteryStateOfCharge(evBatterySocListener)
        ev.registerCurrentEvEfficiency(evEfficiencyListener)
        ev.registerEvChargePortOpen(evChargePortOpenListener)
        ev.getEvBatteryCapacity(
            { capacity -> _snapshot.update { it.copy(batteryCapacityKwh = capacity, timestampMs = now()) } },
            { e -> logFailure("EvBatteryCapacity", e) },
        )
        // batteryEnergyKwh(순간 잔존 kWh): 대응 API 없음 -> null 유지
    }

    private fun registerBrake() {
        val brake = vehicle.getBrake()
        brake.registerBrakePedalStatus(brakePedalListener)
    }

    private fun registerDrivingMode() {
        val dm = vehicle.getDrivingMode()
        dm.registerDrivingModeState(drivingModeListener)
        // 기어·차속은 SDK에 없어서 CarPropertySource에서 받음
    }

    private fun registerHvac() {
        val hvac = vehicle.getHVAC()
        hvac.registerHvacPowerState(driverSeat, hvacPowerListener)
        hvac.registerHvacTemperature(driverSeat, hvacTemperatureListener)
        hvac.registerHvacFanSpeed(driverSeat, hvacFanSpeedListener)
        // outsideTempC: 대응 API 없음 -> null 유지
    }

    private fun registerSeat() {
        val seat = vehicle.getSeat()
        seat.registerHvacSeatTemperature(driverSeat, seatHeatListener)
        seat.registerHvacSeatTemperature(passengerSeat, passengerSeatHeatListener)
    }

    private fun registerOdometer() {
        val odo = vehicle.getOdometer()
        odo.registerDistanceDriven(odometerListener)
        odo.registerTripCurrentDrivingDistance(tripDistanceListener)
        odo.registerTripCurrentDrivingTime(tripDurationListener)
        odo.registerRangeRemainingListener(rangeRemainingListener)
    }

    private fun logFailure(api: String, e: Exception) {
        Log.e(tag, "$api failed", e)
    }

    private fun now() = System.currentTimeMillis()
}
