package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence

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

    @Query("SELECT * FROM offline_map_files WHERE parent IS :parent")
    suspend fun getAllWithParent(parent: Long?): List<OfflineMapFileEntity>

    @Query("SELECT * FROM offline_map_files WHERE _id = :id")
    suspend fun get(id: Long): OfflineMapFileEntity?

    @Upsert
    suspend fun upsert(file: OfflineMapFileEntity): Long

    @Delete
    suspend fun delete(file: OfflineMapFileEntity)

    @Query("DELETE FROM offline_map_files WHERE parent IS :parent")
    suspend fun deleteInGroup(parent: Long?)
}
