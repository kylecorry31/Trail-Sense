package com.kylecorry.trail_sense.tools.maps.infrastructure

import androidx.room.*
import com.kylecorry.trail_sense.tools.maps.domain.MapGroupEntity

@Dao
interface MapGroupDao {
    @Query("SELECT * FROM map_groups WHERE parent IS :parent")
    suspend fun getAllWithParent(parent: Long?): List<MapGroupEntity>

    @Query("SELECT * FROM map_groups WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): MapGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: MapGroupEntity): Long

    @Delete
    suspend fun delete(group: MapGroupEntity)

    @Update
    suspend fun update(group: MapGroupEntity)
}