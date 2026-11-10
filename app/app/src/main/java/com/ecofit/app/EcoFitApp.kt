package com.ecofit.app

import android.app.Application
import com.ecofit.app.data.local.EcoFitDatabase
import com.ecofit.app.data.local.toEntity
import com.ecofit.app.data.navi.NaviRepository
import com.ecofit.app.data.tts.TtsRepository
import com.ecofit.app.data.vehicle.FakeVehicleRepository
import com.ecofit.app.data.vehicle.PleosVehicleRepository
import com.ecofit.app.data.vehicle.SwitchableVehicleRepository
import com.ecofit.app.domain.SavingsCalculator
import com.ecofit.app.domain.TripRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EcoFitApp : Application() {

    companion object {
        // true면 가짜 데이터, false면 Pleos SDK. 시연은 홈의 데모 주행 버튼을 쓴다.
        const val USE_FAKE_VEHICLE = false

        lateinit var instance: EcoFitApp
            private set
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 평소엔 실제 차량, 데모 중엔 대본 데이터를 내보낸다
    lateinit var vehicleRepo: SwitchableVehicleRepository
        private set
    lateinit var naviRepo: NaviRepository
        private set
    lateinit var ttsRepo: TtsRepository
        private set
    lateinit var db: EcoFitDatabase
        private set
    val savings = SavingsCalculator()
    val recorder = TripRecorder()

    override fun onCreate() {
        super.onCreate()
        instance = this
        db = EcoFitDatabase.get(this)
        vehicleRepo = SwitchableVehicleRepository(
            if (USE_FAKE_VEHICLE) FakeVehicleRepository() else PleosVehicleRepository(this)
        )
        vehicleRepo.start()
        naviRepo = NaviRepository(this)
        naviRepo.start()
        ttsRepo = TtsRepository(this)
        ttsRepo.start()

        appScope.launch {
            vehicleRepo.snapshot.collect { s ->
                recorder.feed(s)?.let { trip ->
                    db.tripDao().insert(trip.toEntity())
                }
                // 데모 중엔 달린 거리만큼 가짜 목적지까지 남은 거리를 줄인다
                if (vehicleRepo.isDemo.value) naviRepo.updateDemo(s.odometerKm ?: 0f)
            }
        }
        // 데모가 켜지면 목적지(현대자동차 본사)를 잡아 주고, 끝나면 실제 내비 값으로 돌아간다
        appScope.launch {
            vehicleRepo.isDemo.collect { on -> if (on) naviRepo.startDemo() else naviRepo.stopDemo() }
        }
    }
}
