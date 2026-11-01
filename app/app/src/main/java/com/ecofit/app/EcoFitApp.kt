package com.ecofit.app

import android.app.Application
import com.ecofit.app.data.local.EcoFitDatabase
import com.ecofit.app.data.local.toEntity
import com.ecofit.app.data.vehicle.FakeVehicleRepository
import com.ecofit.app.data.vehicle.PleosVehicleRepository
import com.ecofit.app.data.vehicle.VehicleRepository
import com.ecofit.app.domain.SavingsCalculator
import com.ecofit.app.domain.TripRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EcoFitApp : Application() {

    companion object {
        /** true면 가짜 데이터, false면 Pleos SDK. 에뮬레이터에서 테스트할 때 false로. */
        const val USE_FAKE_VEHICLE = true

        lateinit var instance: EcoFitApp
            private set
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var vehicleRepo: VehicleRepository
        private set
    lateinit var db: EcoFitDatabase
        private set
    val savings = SavingsCalculator()
    val recorder = TripRecorder()

    override fun onCreate() {
        super.onCreate()
        instance = this
        db = EcoFitDatabase.get(this)
        vehicleRepo = if (USE_FAKE_VEHICLE) FakeVehicleRepository() else PleosVehicleRepository(this)
        vehicleRepo.start()

        appScope.launch {
            vehicleRepo.snapshot.collect { s ->
                recorder.feed(s)?.let { trip ->
                    db.tripDao().insert(trip.toEntity())
                }
            }
        }
    }
}
