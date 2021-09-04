package com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trail_sense.shared.paths.PathPoint
import java.time.Instant

class WaypointRepo private constructor(context: Context) : IWaypointRepo {

    private val waypointDao = AppDatabase.getInstance(context).waypointDao()

    override fun getWaypoints() = waypointDao.getAll()

    override suspend fun getWaypoint(id: Long) = waypointDao.get(id)

    override fun getWaypointsByPath(pathId: Long): LiveData<List<PathPoint>> {
        val all = waypointDao.getAllInPath(pathId)
        return Transformations.map(all) { it.map { w -> w.toPathPoint() } }
    }

    override suspend fun deleteWaypoint(waypoint: WaypointEntity) = waypointDao.delete(waypoint)

    override suspend fun deleteOlderThan(instant: Instant) =
        waypointDao.deleteOlderThan(instant.toEpochMilli())

    override suspend fun getLastPathId(): Long? = waypointDao.getLastPathId()

    override suspend fun deletePath(pathId: Long) = waypointDao.deletePath(pathId)

    override suspend fun moveToPath(fromPathId: Long, toPathId: Long) =
        waypointDao.changePath(fromPathId, toPathId)

    override suspend fun addWaypoint(waypoint: WaypointEntity) {
        if (waypoint.id != 0L) {
            waypointDao.update(waypoint)
        } else {
            waypointDao.insert(waypoint)
        }
    }

    companion object {
        private var instance: WaypointRepo? = null

        @Synchronized
        fun getInstance(context: Context): WaypointRepo {
            if (instance == null) {
                instance = WaypointRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}