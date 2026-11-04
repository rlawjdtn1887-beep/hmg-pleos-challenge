package com.ecofit.app.domain

import com.ecofit.app.data.model.TripData

// 지난 주행들을 바탕으로 1km당 얼마나 아끼는지 낸다. 기록이 없으면 기본값을 쓴다.
object RegenHistory {
    const val DEFAULT_WON_PER_KM = 15f
    private const val MIN_DISTANCE_KM = 0.5f

    fun wonPerKm(trips: List<TripData>, wonPerKwh: Float): Float? {
        val dist = trips.sumOf { it.distanceKm.toDouble() }.toFloat()
        if (dist < MIN_DISTANCE_KM) return null
        val regen = trips.sumOf { it.energyRegenKwh.toDouble() }.toFloat()
        return regen * wonPerKwh / dist
    }

    fun wonPerKm(trip: TripData?, wonPerKwh: Float): Float? =
        trip?.takeIf { it.distanceKm >= MIN_DISTANCE_KM }?.let { it.energyRegenKwh * wonPerKwh / it.distanceKm }
}
