package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface VectorMapDao {
    @Query("SELECT * FROM offline_map_files")
    suspend fun getAllSync(): List<VectorMapEntity>

    @Query("SELECT * FROM offline_map_files WHERE parent IS :parent")
    suspend fun getAllWithParent(parent: Long?): List<VectorMapEntity>

    @Query("SELECT * FROM offline_map_files WHERE _id = :id")
    suspend fun get(id: Long): VectorMapEntity?

    @Upsert
    suspend fun upsert(file: VectorMapEntity): Long

    @Delete
    suspend fun delete(file: VectorMapEntity)

    @Query("DELETE FROM offline_map_files WHERE parent IS :parent")
    suspend fun deleteInGroup(parent: Long?)
}
