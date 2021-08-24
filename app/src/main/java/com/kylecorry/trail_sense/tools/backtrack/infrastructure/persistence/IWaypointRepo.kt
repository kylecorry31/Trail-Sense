package com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import java.time.Instant

interface IWaypointRepo {
    fun getWaypoints(): LiveData<List<WaypointEntity>>

    suspend fun getWaypoint(id: Long): WaypointEntity?

    suspend fun deleteWaypoint(waypoint: WaypointEntity)

    suspend fun addWaypoint(waypoint: WaypointEntity)

    suspend fun deleteOlderThan(instant: Instant)

    suspend fun getLastPathId(): Long?

    suspend fun deletePath(pathId: Long)
}