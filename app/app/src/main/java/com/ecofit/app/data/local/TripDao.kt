package com.ecofit.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Query("SELECT * FROM trips ORDER BY startTimeMs DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE startTimeMs >= :sinceMs ORDER BY startTimeMs DESC")
    fun observeSince(sinceMs: Long): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips ORDER BY startTimeMs DESC LIMIT 1")
    fun observeLatest(): Flow<TripEntity?>

    @Query("DELETE FROM trips")
    suspend fun clear()
}
