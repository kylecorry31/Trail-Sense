package com.kylecorry.trail_sense.tools.photo_maps.infrastructure

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapGroupEntity

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