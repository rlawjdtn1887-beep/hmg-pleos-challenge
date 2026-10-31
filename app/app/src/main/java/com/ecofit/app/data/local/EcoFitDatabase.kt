package com.ecofit.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TripEntity::class], version = 1, exportSchema = false)
abstract class EcoFitDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile private var instance: EcoFitDatabase? = null

        fun get(context: Context): EcoFitDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EcoFitDatabase::class.java,
                    "ecofit.db",
                ).build().also { instance = it }
            }
    }
}
