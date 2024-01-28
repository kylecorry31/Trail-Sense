package com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CloudReadingDao {
    @Query("SELECT * FROM clouds")
    fun getAll(): LiveData<List<CloudReadingEntity>>

    @Query("SELECT * FROM clouds")
    suspend fun getAllSync(): List<CloudReadingEntity>

    @Query("SELECT * FROM clouds WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): CloudReadingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cloud: CloudReadingEntity): Long

    @Delete
    suspend fun delete(cloud: CloudReadingEntity)

    @Query("DELETE FROM clouds WHERE time < :minEpochMillis")
    suspend fun deleteOlderThan(minEpochMillis: Long)

    @Update
    suspend fun update(cloud: CloudReadingEntity)
}