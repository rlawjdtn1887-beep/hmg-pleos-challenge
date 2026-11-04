package com.ecofit.app.data.vehicle

import com.ecofit.app.data.model.VehicleSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// 평소엔 실제 차량 데이터를, 데모 중엔 대본 데이터를 내보낸다.
// 화면과 TripRecorder는 이것 하나만 보면 된다.
@OptIn(ExperimentalCoroutinesApi::class)
class SwitchableVehicleRepository(
    private val real: VehicleRepository,
) : VehicleRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val demo = MutableStateFlow<DemoVehicleRepository?>(null)

    val isDemo: StateFlow<Boolean> = demo
        .map { it != null }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override val snapshot: StateFlow<VehicleSnapshot> = demo
        .flatMapLatest { d -> d?.snapshot ?: real.snapshot }
        .stateIn(scope, SharingStarted.Eagerly, real.snapshot.value)

    override val isConnected: StateFlow<Boolean> = demo
        .flatMapLatest { d -> d?.isConnected ?: real.isConnected }
        .stateIn(scope, SharingStarted.Eagerly, real.isConnected.value)

    override fun start() = real.start()

    override fun stop() {
        stopDemo()
        real.stop()
    }

    fun startDemo() {
        if (demo.value != null) return
        val d = DemoVehicleRepository(onFinished = { stopDemo() })
        demo.value = d
        d.start()
    }

    fun stopDemo() {
        val d = demo.value ?: return
        demo.value = null
        d.stop()
    }
}
