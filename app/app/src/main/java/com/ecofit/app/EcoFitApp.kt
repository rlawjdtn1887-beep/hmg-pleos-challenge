package com.ecofit.app

import android.app.Application
import com.ecofit.app.data.local.EcoFitDatabase
import com.ecofit.app.data.local.toEntity
import com.ecofit.app.data.navi.NaviRepository
import com.ecofit.app.data.tts.TtsRepository
import com.ecofit.app.data.vehicle.FakeVehicleRepository
import com.ecofit.app.data.vehicle.PleosVehicleRepository
import com.ecofit.app.data.vehicle.SwitchableVehicleRepository
import com.ecofit.app.domain.Coaching
import com.ecofit.app.domain.SavingsCalculator
import com.ecofit.app.domain.TripRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

        // 데모가 저배터리 구간을 지나가면 한 번만 충전소 화면을 띄운다. 화면(Compose) 쪽
        // LaunchedEffect에 맡기면 데모가 곧장 P로 끝나며 그 화면이 사라질 때 지연 호출도
        // 함께 취소돼 버려서, 화면 전환과 무관한 앱 스코프에서 다룬다
        var demoLowBatteryHandled = false
        appScope.launch {
            vehicleRepo.isDemo.collect { on -> if (!on) demoLowBatteryHandled = false }
        }
        appScope.launch {
            vehicleRepo.snapshot.collect { s ->
                recorder.feed(s, isDemo = vehicleRepo.isDemo.value)?.let { trip ->
                    db.tripDao().insert(trip.toEntity())
                }
                // 데모 중엔 달린 거리만큼 가짜 목적지까지 남은 거리를 줄인다
                if (vehicleRepo.isDemo.value) {
                    naviRepo.updateDemo(s.odometerKm ?: 0f)
                    if (!demoLowBatteryHandled && Coaching.isLowBattery(s.batterySocPercent)) {
                        demoLowBatteryHandled = true
                        appScope.launch {
                            delay(1500)
                            naviRepo.showNearbyChargingStations()
                        }
                    }
                }
            }
        }
        // 데모가 켜지면 목적지(현대자동차 본사)를 잡아 주고, 끝나면 실제 내비 값으로 돌아간다
        appScope.launch {
            vehicleRepo.isDemo.collect { on -> if (on) naviRepo.startDemo() else naviRepo.stopDemo() }
        }
    }
}
