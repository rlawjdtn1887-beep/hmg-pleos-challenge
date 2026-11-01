package com.ecofit.app.data.vehicle

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import com.ecofit.app.data.model.Gear

// Pleos Vehicle SDK에 기어·차속이 없어서 AAOS CarPropertyManager로 직접 받는다.
// PERF_VEHICLE_SPEED 는 CAR_SPEED(런타임 권한), GEAR_SELECTION 은 CAR_POWERTRAIN 필요.
class CarPropertySource(
    private val context: Context,
    private val onSpeedKmh: (Float) -> Unit,
    private val onGear: (Gear) -> Unit,
) {
    private val tag = "CarPropertySource"
    private var car: Car? = null
    private var manager: CarPropertyManager? = null
    private val registered = mutableSetOf<Int>()

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            when (value.propertyId) {
                VehiclePropertyIds.PERF_VEHICLE_SPEED ->
                    (value.value as? Float)?.let { onSpeedKmh(it * 3.6f) }
                VehiclePropertyIds.GEAR_SELECTION ->
                    (value.value as? Int)?.let { onGear(toGear(it)) }
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.w(tag, "property error id=$propertyId area=$areaId")
        }
    }

    // 권한 허용 후 다시 불러도 되도록 안 된 것만 등록한다
    fun start() {
        val m = manager ?: run {
            val c = Car.createCar(context.applicationContext) ?: return
            car = c
            (c.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager)?.also { manager = it }
        } ?: return

        register(m, VehiclePropertyIds.PERF_VEHICLE_SPEED, 5f)
        register(m, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
    }

    private fun register(m: CarPropertyManager, id: Int, rate: Float) {
        if (id in registered) return
        try {
            if (m.registerCallback(callback, id, rate)) registered += id
        } catch (e: SecurityException) {
            Log.w(tag, "no permission for $id")
        } catch (e: Exception) {
            Log.w(tag, "register failed for $id", e)
        }
    }

    fun stop() {
        manager?.unregisterCallback(callback)
        registered.clear()
        car?.disconnect()
        car = null
        manager = null
    }

    // android.car.VehicleGear 값
    private fun toGear(v: Int): Gear = when (v) {
        1 -> Gear.N
        2 -> Gear.R
        4 -> Gear.P
        8 -> Gear.D
        0 -> Gear.UNKNOWN
        else -> if (v >= 16) Gear.D else Gear.UNKNOWN
    }
}
