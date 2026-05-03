package com.kylecorry.trail_sense.tools.map.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineMapFileDao {
    @Query("SELECT * FROM offline_map_files")
    fun getAll(): Flow<List<OfflineMapFileEntity>>

    @Query("SELECT * FROM offline_map_files")
    suspend fun getAllSync(): List<OfflineMapFileEntity>

    @Upsert
    suspend fun upsert(file: OfflineMapFileEntity): Long

    @Delete
    suspend fun delete(file: OfflineMapFileEntity)
}
