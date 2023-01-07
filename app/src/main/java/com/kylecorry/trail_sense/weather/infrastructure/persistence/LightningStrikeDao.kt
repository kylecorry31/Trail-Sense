package com.kylecorry.trail_sense.weather.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface LightningStrikeDao {
    @Query("SELECT * FROM lightning")
    fun getAll(): LiveData<List<LightningStrikeEntity>>

    @Query("SELECT * FROM lightning")
    suspend fun getAllSync(): List<LightningStrikeEntity>

    @Query("SELECT * FROM lightning WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): LightningStrikeEntity?

    @Query("SELECT * FROM lightning ORDER BY _id DESC LIMIT 1")
    suspend fun getLast(): LightningStrikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(strike: LightningStrikeEntity): Long

    @Delete
    suspend fun delete(strike: LightningStrikeEntity)

    @Query("DELETE FROM lightning WHERE time < :minEpochMillis")
    suspend fun deleteOlderThan(minEpochMillis: Long)

    @Update
    suspend fun update(strike: LightningStrikeEntity)
}