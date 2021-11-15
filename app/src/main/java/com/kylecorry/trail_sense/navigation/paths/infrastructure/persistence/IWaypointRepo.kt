package com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.shared.paths.PathPoint
import java.time.Instant

interface IWaypointRepo {
    suspend fun add(value: PathPoint): Long

    suspend fun delete(value: PathPoint)

    suspend fun addAll(value: List<PathPoint>)

    suspend fun deleteAll(value: List<PathPoint>)

    suspend fun deleteInPath(pathId: Long)

    suspend fun deleteOlderInPath(pathId: Long, time: Instant)

    suspend fun get(id: Long): PathPoint?

    suspend fun getAll(): List<PathPoint>

    fun getAllLive(since: Instant? = null): LiveData<List<PathPoint>>

    suspend fun getAllInPaths(pathIds: List<Long>): List<PathPoint>

    fun getAllInPathLive(pathId: Long): LiveData<List<PathPoint>>
}