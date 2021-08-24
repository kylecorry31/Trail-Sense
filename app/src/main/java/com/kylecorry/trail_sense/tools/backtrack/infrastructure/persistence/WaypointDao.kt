package com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity

@Dao
interface WaypointDao {
    @Query("SELECT * FROM waypoints")
    fun getAll(): LiveData<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): WaypointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(waypoint: WaypointEntity): Long

    @Delete
    suspend fun delete(waypoint: WaypointEntity)

    @Query("DELETE FROM waypoints WHERE createdOn < :minEpochMillis")
    suspend fun deleteOlderThan(minEpochMillis: Long)

    @Update
    suspend fun update(waypoint: WaypointEntity)

    @Query("SELECT MAX(pathId) FROM waypoints")
    suspend fun getLastPathId(): Long?

    @Query("DELETE FROM waypoints WHERE pathId = :pathId")
    suspend fun deletePath(pathId: Long)
}