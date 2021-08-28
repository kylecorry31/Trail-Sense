package com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trailsensecore.domain.geo.PathPoint
import java.time.Instant

interface IWaypointRepo {
    fun getWaypoints(): LiveData<List<WaypointEntity>>

    suspend fun getWaypoint(id: Long): WaypointEntity?

    fun getWaypointsByPath(pathId: Long): LiveData<List<PathPoint>>

    suspend fun deleteWaypoint(waypoint: WaypointEntity)

    suspend fun addWaypoint(waypoint: WaypointEntity)

    suspend fun deleteOlderThan(instant: Instant)

    suspend fun getLastPathId(): Long?

    suspend fun deletePath(pathId: Long)

    suspend fun moveToPath(fromPathId: Long, toPathId: Long)
}