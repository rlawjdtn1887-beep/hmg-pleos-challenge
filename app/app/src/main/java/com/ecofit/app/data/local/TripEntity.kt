package com.ecofit.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ecofit.app.data.model.TripData

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
)

fun TripData.toEntity() = TripEntity(
    id, startTimeMs, endTimeMs, distanceKm, startSocPercent, endSocPercent,
    energyUsedKwh, energyRegenKwh, hardBrakeCount, hardAccelCount, hvacOnSec, seatHeatOnSec, avgOutsideTempC,
)

fun TripEntity.toModel() = TripData(
    id, startTimeMs, endTimeMs, distanceKm, startSocPercent, endSocPercent,
    energyUsedKwh, energyRegenKwh, hardBrakeCount, hardAccelCount, hvacOnSec, seatHeatOnSec, avgOutsideTempC,
)
