package com.kylecorry.trail_sense.shared.dem

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DigitalElevationModelDao {

    @Query("SELECT * FROM dem")
    suspend fun getAll(): List<DigitalElevationModelEntity>

    @Upsert
    suspend fun upsert(dem: List<DigitalElevationModelEntity>)

    @Query("DELETE FROM dem")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(tile: DigitalElevationModelEntity)

    @Query("SELECT version FROM dem LIMIT 1")
    suspend fun getVersion(): String?

}