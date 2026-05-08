package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapEntity

@Dao
interface MapDao {
    @Query("SELECT * FROM maps")
    suspend fun getAll(): List<MapEntity>

    @Query("SELECT * FROM maps where parent IS :parent")
    suspend fun getAllWithParent(parent: Long?): List<MapEntity>

    @Query("SELECT * FROM maps WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): MapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(map: MapEntity): Long

    @Delete
    suspend fun delete(map: MapEntity)

    @Query("DELETE FROM maps WHERE parent is :parent")
    suspend fun deleteInGroup(parent: Long?)

    @Update
    suspend fun update(map: MapEntity)
}
