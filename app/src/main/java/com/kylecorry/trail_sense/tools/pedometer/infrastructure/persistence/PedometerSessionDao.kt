package com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

// #1397: DAO for pedometer session CRUD and range queries
@Dao
interface PedometerSessionDao {
    @Query("SELECT * FROM pedometer_sessions ORDER BY start_time DESC")
    suspend fun getAllSync(): List<PedometerSessionEntity>

    @Query("SELECT * FROM pedometer_sessions WHERE start_time >= :from AND start_time < :to ORDER BY start_time ASC")
    suspend fun getRange(from: Long, to: Long): List<PedometerSessionEntity>

    @Query("SELECT * FROM pedometer_sessions WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): PedometerSessionEntity?

    @Query("SELECT * FROM pedometer_sessions ORDER BY start_time ASC LIMIT 1")
    suspend fun getEarliest(): PedometerSessionEntity?

    @Upsert
    suspend fun upsert(session: PedometerSessionEntity): Long

    @Delete
    suspend fun delete(session: PedometerSessionEntity)
}
