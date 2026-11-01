package com.ecofit.app.domain

import com.ecofit.app.data.model.TripData
import kotlin.math.roundToInt

data class TripSavings(
    val regenKwh: Float,
    val savedWon: Int,
    val savedKm: Float,
    val coffeeCups: Float,
    val efficiencyKmPerKwh: Float?,
    val vsBaselinePercent: Int?,
)

/**
 * 트립 데이터를 사람이 이해하는 숫자로 바꾼다.
 * 단가는 기획서에 출처와 함께 적을 것. 여기 값은 초기값.
 */
class SavingsCalculator(
    private val wonPerKwh: Float = 350f,
    private val coffeeWon: Float = 4500f,
    private val baselineKmPerKwh: Float = 4.5f,
) {
    fun calculate(trip: TripData): TripSavings {
        val regen = trip.energyRegenKwh
        val eff = trip.efficiencyKmPerKwh
        val savedKm = regen * (eff ?: baselineKmPerKwh)
        val savedWon = (regen * wonPerKwh).roundToInt()
        val vsBaseline = eff?.let { ((it - baselineKmPerKwh) / baselineKmPerKwh * 100).roundToInt() }
        return TripSavings(
            regenKwh = regen,
            savedWon = savedWon,
            savedKm = savedKm,
            coffeeCups = savedWon / coffeeWon,
            efficiencyKmPerKwh = eff,
            vsBaselinePercent = vsBaseline,
        )
    }

    fun weekly(trips: List<TripData>): TripSavings {
        val totalRegen = trips.sumOf { it.energyRegenKwh.toDouble() }.toFloat()
        val totalDist = trips.sumOf { it.distanceKm.toDouble() }.toFloat()
        val totalNet = trips.sumOf { it.netEnergyKwh.toDouble() }.toFloat()
        val eff = if (totalNet > 0.01f) totalDist / totalNet else null
        val savedWon = (totalRegen * wonPerKwh).roundToInt()
        return TripSavings(
            regenKwh = totalRegen,
            savedWon = savedWon,
            savedKm = totalRegen * (eff ?: baselineKmPerKwh),
            coffeeCups = savedWon / coffeeWon,
            efficiencyKmPerKwh = eff,
            vsBaselinePercent = eff?.let { ((it - baselineKmPerKwh) / baselineKmPerKwh * 100).roundToInt() },
        )
    }
}
