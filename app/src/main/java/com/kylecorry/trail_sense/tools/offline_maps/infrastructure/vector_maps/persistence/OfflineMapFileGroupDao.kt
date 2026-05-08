package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface OfflineMapFileGroupDao {
    @Query("SELECT * FROM offline_map_file_groups WHERE parent IS :parent")
    suspend fun getAllWithParent(parent: Long?): List<OfflineMapFileGroupEntity>

    @Query("SELECT * FROM offline_map_file_groups WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): OfflineMapFileGroupEntity?

    @Upsert
    suspend fun upsert(group: OfflineMapFileGroupEntity): Long

    @Delete
    suspend fun delete(group: OfflineMapFileGroupEntity)
}
