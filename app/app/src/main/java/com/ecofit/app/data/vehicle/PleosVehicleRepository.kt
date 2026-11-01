package com.ecofit.app.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import android.content.Context
import android.util.Log
import com.ecofit.app.data.model.VehicleSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Pleos Vehicle SDK 연동. 담당: 정수
 *
 * 구조:
 *   start()  -> vehicle.initialize() -> 각 도메인 capability 확인 -> register 콜백
 *   콜백     -> _snapshot.update { copy(...) }
 *   stop()   -> unregister -> vehicle.release()
 *
 * 각 도메인의 정확한 메서드/리스너 이름은 Android Studio에서
 * vehicle.getEvBattery(). 까지 치고 자동완성으로 확인.
 * 샘플: https://github.com/Pleos-Playground/pleos-sample-vehicle-app (VehicleClient.kt 의 Door 패턴과 동일)
 */
class PleosVehicleRepository(context: Context) : VehicleRepository {

    private val tag = "PleosVehicleRepo"
    private val vehicle = Vehicle(context.applicationContext)
    private var initialized = false

    private val _snapshot = MutableStateFlow(VehicleSnapshot())
    override val snapshot: StateFlow<VehicleSnapshot> = _snapshot.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val carProps = CarPropertySource(
        context,
        onSpeedKmh = { v -> _snapshot.update { it.copy(speedKmh = v, timestampMs = now()) } },
        onGear = { g -> _snapshot.update { it.copy(gear = g, timestampMs = now()) } },
    )

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
            // TODO(정수): 등록한 리스너 전부 unregister
            vehicle.release()
        } catch (e: Exception) {
            Log.e(tag, "release failed", e)
        }
        initialized = false
        _isConnected.value = false
    }

    // ---- 도메인별 등록. 패턴은 전부 같다: capability 확인 -> register(listener) ----

    private fun registerEvBattery() {
        val ev = vehicle.getEvBattery()
        // TODO(정수):
        //  ev.checkXxxCapabilities(...)
        //  ev.registerEvBatteryStateOfCharge(listener)   -> soc
        //  ev.registerXxx(...)                           -> batteryEnergyKwh, remainingRangeKm, isCharging
        //  ev.getCurrentEvEfficiency(...)                -> evEfficiencyKmPerKwh
        //
        //  리스너 안에서:
        //  _snapshot.update { it.copy(batterySocPercent = value, timestampMs = now()) }
    }

    private fun registerBrake() {
        val brake = vehicle.getBrake()
        // TODO(정수): brake pedal position(%) 콜백 -> brakePedalPercent
    }

    private fun registerDrivingMode() {
        val dm = vehicle.getDrivingMode()
        // TODO(정수): drive mode(에코/노멀/스포츠) 콜백 -> driveMode
        //  기어·차속은 SDK에 없어서 CarPropertySource에서 받음
    }

    private fun registerHvac() {
        val hvac = vehicle.getHvac()
        // TODO(정수): power on/off, target temp, outside temp, fan speed
        //  v2.3.x에서 registerHvacTemperature 시그니처 바뀜(HvacTemperatureValueListener) 주의
    }

    private fun registerSeat() {
        val seat = vehicle.getSeat()
        // TODO(정수): driver seat heat level -> driverSeatHeatLevel
    }

    private fun registerOdometer() {
        val odo = vehicle.getOdometer()
        // TODO(정수): odometer, trip distance/duration -> odometerKm, tripDistanceKm, tripDurationSec
    }

    private fun now() = System.currentTimeMillis()
}
