package com.ecofit.app.domain

import com.ecofit.app.data.model.TripData

// 급제동을 줄였을 때 얼마나 더 갈 수 있었는지. 급제동 한 번에 못 되찾는 에너지를 전비로 km로 바꾼다
object Coaching {
    private const val LOST_WH_PER_HARD_BRAKE = 20f
    const val BASELINE_KM_PER_KWH = 4.5f
    const val LOW_BATTERY_PERCENT = 20f

    fun extraKmWithoutHardBrakes(t: TripData): Float {
        if (t.hardBrakeCount == 0) return 0f
        val eff = t.efficiencyKmPerKwh ?: BASELINE_KM_PER_KWH
        return t.hardBrakeCount * LOST_WH_PER_HARD_BRAKE / 1000f * eff
    }

    fun isLowBattery(soc: Float?) = soc != null && soc < LOW_BATTERY_PERCENT
}
