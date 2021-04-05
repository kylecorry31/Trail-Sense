package com.kylecorry.trail_sense.tools.maps.infrastructure

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trail_sense.tools.maps.domain.MapEntity

@Dao
interface MapDao {
    @Query("SELECT * FROM maps")
    fun getAll(): LiveData<List<MapEntity>>

    @Query("SELECT * FROM maps WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): MapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(map: MapEntity): Long

    @Delete
    suspend fun delete(map: MapEntity)

    @Update
    suspend fun update(map: MapEntity)
}