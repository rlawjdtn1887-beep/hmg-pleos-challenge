package com.ecofit.app.domain

import com.ecofit.app.data.model.TripData

// 주행 하나를 0~100점으로 요약한다. 급제동과 급가속만큼 깎고, 기준보다 전비가 좋으면 더해준다.
object EcoScore {
    private const val BRAKE_PENALTY = 6
    private const val ACCEL_PENALTY = 3
    private const val EFFICIENCY_BONUS_MAX = 10
    private const val BASELINE_KM_PER_KWH = 4.5f

    fun of(t: TripData): Int {
        var score = 100
        score -= t.hardBrakeCount * BRAKE_PENALTY
        score -= t.hardAccelCount * ACCEL_PENALTY
        t.efficiencyKmPerKwh?.let { eff ->
            val gain = ((eff - BASELINE_KM_PER_KWH) / BASELINE_KM_PER_KWH * 50f).toInt()
            score += gain.coerceIn(-EFFICIENCY_BONUS_MAX, EFFICIENCY_BONUS_MAX)
        }
        return score.coerceIn(0, 100)
    }

    fun label(score: Int) = when {
        score >= 90 -> "훌륭해요"
        score >= 70 -> "좋아요"
        score >= 50 -> "보통이에요"
        else -> "연습이 필요해요"
    }
}
