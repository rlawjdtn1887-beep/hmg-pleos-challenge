package com.ecofit.app.data.model

/**
 * 한 번의 주행(기어 D 진입 ~ P 복귀) 결과.
 * TripRecorder가 스냅샷 스트림을 보고 만들어서 DB에 저장한다.
 */
data class TripData(
    val id: Long = 0,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val distanceKm: Float,

    val startSocPercent: Float?,
    val endSocPercent: Float?,
    val energyUsedKwh: Float,
    val energyRegenKwh: Float,

    val hardBrakeCount: Int,
    val hardAccelCount: Int,

    val hvacOnSec: Long,
    val seatHeatOnSec: Long,
    val avgOutsideTempC: Float?,
) {
    val durationSec: Long get() = (endTimeMs - startTimeMs) / 1000

    val netEnergyKwh: Float get() = energyUsedKwh - energyRegenKwh

    val efficiencyKmPerKwh: Float?
        get() = if (netEnergyKwh > 0.01f) distanceKm / netEnergyKwh else null
}
