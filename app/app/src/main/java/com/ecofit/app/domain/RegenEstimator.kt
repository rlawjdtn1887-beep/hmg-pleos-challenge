package com.ecofit.app.domain

// SoC가 정수(%)라서 감속 한 번의 회생량은 SoC로 못 잰다. 차속 변화로 추정한다.
// 감속 구간에서 줄어든 운동에너지 - 구름·공기 저항 손실 = 회생 가능 에너지, 여기에 회생 효율을 곱한다.
// 기본값은 아이오닉 5 롱레인지 기준 근사치.
class RegenEstimator(
    private val massKg: Float = 2050f,
    private val efficiency: Float = 0.65f,
    private val maxRegenKw: Float = 100f,
    private val rollingCoef: Float = 0.011f,
    private val dragArea: Float = 0.65f,
) {
    private val g = 9.81f
    private val airDensity = 1.2f

    // 반환: 이 구간에서 회수한 kWh. 감속이 아니면 0
    fun step(v0Kmh: Float, v1Kmh: Float, dtSec: Float): Float {
        if (dtSec <= 0f || v1Kmh >= v0Kmh) return 0f
        val v0 = v0Kmh / 3.6f
        val v1 = v1Kmh / 3.6f
        val vAvg = (v0 + v1) / 2f

        val kineticJ = 0.5f * massKg * (v0 * v0 - v1 * v1)
        val lossForce = massKg * g * rollingCoef + 0.5f * airDensity * dragArea * vAvg * vAvg
        val lossJ = lossForce * vAvg * dtSec

        val recoverable = (kineticJ - lossJ).coerceAtLeast(0f) * efficiency
        val capJ = maxRegenKw * 1000f * dtSec
        return recoverable.coerceAtMost(capJ) / 3_600_000f
    }

    // m/s². 양수면 가속, 음수면 감속
    fun accel(v0Kmh: Float, v1Kmh: Float, dtSec: Float): Float =
        if (dtSec <= 0f) 0f else (v1Kmh - v0Kmh) / 3.6f / dtSec
}
