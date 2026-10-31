package com.ecofit.app.data.vehicle

import com.ecofit.app.data.model.VehicleSnapshot
import kotlinx.coroutines.flow.StateFlow

/**
 * 차량 데이터 소스 추상화.
 * - PleosVehicleRepository: 실제 SDK (정수)
 * - FakeVehicleRepository: 에뮬레이터 없이 UI 개발용 (찬서)
 */
interface VehicleRepository {
    val snapshot: StateFlow<VehicleSnapshot>
    val isConnected: StateFlow<Boolean>

    fun start()
    fun stop()
}
