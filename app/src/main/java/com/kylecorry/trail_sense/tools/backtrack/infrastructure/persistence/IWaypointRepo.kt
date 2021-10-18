package com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.shared.database.ICleanable
import com.kylecorry.trail_sense.shared.paths.PathPoint
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity

interface IWaypointRepo: ICleanable {
    fun getWaypoints(): LiveData<List<WaypointEntity>>

    suspend fun getWaypointsSync(): List<WaypointEntity>

    suspend fun getWaypoint(id: Long): WaypointEntity?

    fun getWaypointsByPath(pathId: Long): LiveData<List<PathPoint>>

    suspend fun deleteWaypoint(waypoint: WaypointEntity)

    suspend fun addWaypoint(waypoint: WaypointEntity)

    suspend fun getLastPathId(): Long?

    suspend fun deletePath(pathId: Long)

    suspend fun moveToPath(fromPathId: Long, toPathId: Long)
}