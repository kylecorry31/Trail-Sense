package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.shared.paths.PathPoint
import java.time.Instant

interface IWaypointRepo {
    suspend fun add(value: PathPoint): Long

    suspend fun delete(value: PathPoint)

    suspend fun deleteInPath(pathId: Long)

    suspend fun deleteOlderInPath(pathId: Long, time: Instant)

    suspend fun get(id: Long): PathPoint?

    suspend fun getAll(): List<PathPoint>

    fun getAllLive(): LiveData<List<PathPoint>>

    suspend fun getAllInPaths(pathIds: List<Long>): List<PathPoint>
}