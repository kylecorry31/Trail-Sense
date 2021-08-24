package com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence

import android.content.Context
import com.kylecorry.trail_sense.shared.AppDatabase
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import java.time.Instant

class WaypointRepo private constructor(context: Context) : IWaypointRepo {

    private val waypointDao = AppDatabase.getInstance(context).waypointDao()

    override fun getWaypoints() = waypointDao.getAll()

    override suspend fun getWaypoint(id: Long) = waypointDao.get(id)

    override suspend fun deleteWaypoint(waypoint: WaypointEntity) = waypointDao.delete(waypoint)

    override suspend fun deleteOlderThan(instant: Instant) =
        waypointDao.deleteOlderThan(instant.toEpochMilli())

    override suspend fun getLastPathId(): Long? = waypointDao.getLastPathId()

    override suspend fun deletePath(pathId: Long) = waypointDao.deletePath(pathId)

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