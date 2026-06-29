package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface TrailMapDao {
    @Query("SELECT * FROM offline_map_files")
    suspend fun getAllSync(): List<TrailMapEntity>

    @Query("SELECT * FROM offline_map_files WHERE parent IS :parent")
    suspend fun getAllWithParent(parent: Long?): List<TrailMapEntity>

    @Query("SELECT * FROM offline_map_files WHERE _id = :id")
    suspend fun get(id: Long): TrailMapEntity?

    @Upsert
    suspend fun upsert(file: TrailMapEntity): Long

    @Delete
    suspend fun delete(file: TrailMapEntity)

    @Query("DELETE FROM offline_map_files WHERE parent IS :parent")
    suspend fun deleteInGroup(parent: Long?)
}
