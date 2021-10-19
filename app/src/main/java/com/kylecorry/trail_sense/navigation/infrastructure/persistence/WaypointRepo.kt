package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.shared.paths.PathPoint
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import java.time.Instant

class WaypointRepo private constructor(context: Context) : IWaypointRepo {

    private val waypointDao = AppDatabase.getInstance(context).waypointDao()

    override suspend fun add(value: PathPoint): Long {
        return if (value.id != 0L) {
            waypointDao.update(WaypointEntity.from(value))
            value.id
        } else {
            waypointDao.insert(WaypointEntity.from(value))
        }
    }

    override suspend fun delete(value: PathPoint) {
        waypointDao.delete(WaypointEntity.from(value))
    }

    override suspend fun deleteInPath(pathId: Long) {
        waypointDao.deleteByPath(pathId)
    }

    override suspend fun deleteOlderInPath(pathId: Long, time: Instant) {
        waypointDao.deleteOlderThan(time.toEpochMilli(), pathId)
    }

    override suspend fun get(id: Long): PathPoint? {
        return waypointDao.get(id)?.toPathPoint()
    }

    override suspend fun getAll(): List<PathPoint> {
        return waypointDao.getAllSync().map { it.toPathPoint() }
    }

    override fun getAllLive(): LiveData<List<PathPoint>> {
        return Transformations.map(waypointDao.getAll()) {
            it.map { waypoint -> waypoint.toPathPoint() }
        }
    }

    override suspend fun getAllInPaths(pathIds: List<Long>): List<PathPoint> {
        val points = mutableListOf<WaypointEntity>()
        for (pathId in pathIds){
            points.addAll(waypointDao.getAllInPathSync(pathId))
        }
        return points.map { it.toPathPoint() }
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