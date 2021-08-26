package com.kylecorry.trail_sense.weather.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PressureReadingDao {
    @Query("SELECT * FROM pressures")
    fun getAll(): LiveData<List<PressureReadingEntity>>

    @Query("SELECT * FROM pressures")
    suspend fun getAllSync(): List<PressureReadingEntity>

    @Query("SELECT * FROM pressures WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): PressureReadingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pressure: PressureReadingEntity): Long

    @Delete
    suspend fun delete(pressure: PressureReadingEntity)

    @Query("DELETE FROM pressures WHERE time < :minEpochMillis")
    suspend fun deleteOlderThan(minEpochMillis: Long)

    @Update
    suspend fun update(pressure: PressureReadingEntity)
}