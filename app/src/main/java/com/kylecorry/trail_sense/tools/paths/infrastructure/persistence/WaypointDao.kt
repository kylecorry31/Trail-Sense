package com.kylecorry.trail_sense.tools.paths.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kylecorry.trail_sense.tools.paths.domain.WaypointEntity

@Dao
interface WaypointDao {
    @Query("SELECT * FROM waypoints")
    fun getAll(): LiveData<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE createdOn > :since")
    fun getAllSince(since: Long): LiveData<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints")
    suspend fun getAllSync(): List<WaypointEntity>

    @Query("SELECT * FROM waypoints WHERE pathId = :pathId")
    fun getAllInPath(pathId: Long): LiveData<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE pathId = :pathId")
    suspend fun getAllInPathSync(pathId: Long): List<WaypointEntity>

    @Query("SELECT * FROM waypoints WHERE pathId = :pathId ORDER BY _id ASC LIMIT 1")
    suspend fun getFirstInPath(pathId: Long): WaypointEntity?

    @Query("SELECT * FROM waypoints WHERE pathId = :pathId ORDER BY _id DESC LIMIT 1")
    suspend fun getLastInPath(pathId: Long): WaypointEntity?

    @Query(
        "SELECT _id FROM paths WHERE " +
                "(SELECT MAX(altitude) - MIN(altitude) FROM waypoints WHERE pathId = paths._id) >= :minChange " +
                "ORDER BY _id DESC LIMIT 1"
    )
    suspend fun getMostRecentPathIdWithElevationChange(minChange: Float): Long?

    @Query("SELECT COUNT(*) FROM waypoints WHERE pathId = :pathId")
    suspend fun getCountInPath(pathId: Long): Int

    @Query("SELECT * FROM waypoints WHERE cellQuality IS NOT NULL")
    suspend fun getAllWithCellSignal(): List<WaypointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(waypoint: WaypointEntity): Long

    @Delete
    suspend fun delete(waypoint: WaypointEntity)

    @Query("DELETE FROM waypoints WHERE createdOn < :minEpochMillis AND pathId = :pathId")
    suspend fun deleteOlderThan(minEpochMillis: Long, pathId: Long)

    @Update
    suspend fun update(waypoint: WaypointEntity)

    @Query("DELETE FROM waypoints WHERE pathId = :pathId")
    suspend fun deleteByPath(pathId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsert(waypoints: List<WaypointEntity>)

    @Update
    suspend fun bulkUpdate(waypoints: List<WaypointEntity>)

    @Delete
    suspend fun bulkDelete(waypoints: List<WaypointEntity>)
}
