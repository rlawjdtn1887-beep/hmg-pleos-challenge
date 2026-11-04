package com.ecofit.app.data.model

enum class Gear { P, R, N, D, UNKNOWN }

enum class DriveMode { ECO, NORMAL, SPORT, UNKNOWN }

/**
 * 차량 상태 한 시점의 스냅샷.
 * VehicleRepository가 SDK 콜백을 받을 때마다 갱신해서 Flow로 내보낸다.
 * null은 해당 도메인을 차종이 지원하지 않거나 아직 값이 안 들어온 경우.
 */
data class VehicleSnapshot(
    val timestampMs: Long = System.currentTimeMillis(),

    // EvBattery
    val batterySocPercent: Float? = null,
    val batteryEnergyKwh: Float? = null,
    val batteryCapacityKwh: Float? = null,
    val remainingRangeKm: Float? = null,
    val evEfficiencyKmPerKwh: Float? = null,
    val isCharging: Boolean? = null,

    // Brake
    val brakePedalPercent: Float? = null,

    // AAOS CarPropertyManager (Pleos SDK에 없음)
    val speedKmh: Float? = null,

    // DrivingMode
    val gear: Gear = Gear.UNKNOWN,
    val driveMode: DriveMode = DriveMode.UNKNOWN,

    // HVAC
    val hvacOn: Boolean? = null,
    val hvacTargetTempC: Float? = null,
    val outsideTempC: Float? = null,
    val fanSpeed: Int? = null,

    // Seat
    val driverSeatHeatLevel: Int? = null,
    val passengerSeatHeatLevel: Int? = null,

    // Odometer
    val odometerKm: Float? = null,
    val tripDistanceKm: Float? = null,
    val tripDurationSec: Long? = null,
)
