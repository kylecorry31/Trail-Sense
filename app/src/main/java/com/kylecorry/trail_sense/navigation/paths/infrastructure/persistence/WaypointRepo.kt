package com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.domain.WaypointEntity
import com.kylecorry.trail_sense.shared.database.AppDatabase
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

    override suspend fun addAll(value: List<PathPoint>) {
        val toAdd = value.filter { it.id == 0L }.map { WaypointEntity.from(it) }
        val toUpdate = value.filter { it.id != 0L }.map { WaypointEntity.from(it) }

        if (toAdd.isNotEmpty()) {
            waypointDao.bulkInsert(toAdd)
        }

        if (toUpdate.isNotEmpty()) {
            waypointDao.bulkUpdate(toUpdate)
        }
    }

    override suspend fun deleteAll(value: List<PathPoint>) {
        waypointDao.bulkDelete(value.map { WaypointEntity.from(it) })
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

    override suspend fun getAll(since: Instant?): List<PathPoint> {
        return (if (since == null) waypointDao.getAllSync() else waypointDao.getAllSinceSync(since.toEpochMilli()))
            .map { it.toPathPoint() }
    }

    override fun getAllLive(since: Instant?): LiveData<List<PathPoint>> {
        return if (since == null) {
            waypointDao.getAll()
        } else {
            waypointDao.getAllSince(
                since.toEpochMilli()
            )
        }.map {
            it.map { waypoint -> waypoint.toPathPoint() }
        }
    }

    override suspend fun getAllInPaths(pathIds: List<Long>): List<PathPoint> {
        val points = mutableListOf<WaypointEntity>()
        for (pathId in pathIds) {
            points.addAll(waypointDao.getAllInPathSync(pathId))
        }
        return points.map { it.toPathPoint() }
    }

    override fun getAllInPathLive(pathId: Long): LiveData<List<PathPoint>> {
        return waypointDao.getAllInPath(pathId).map {
            it.map { waypoint -> waypoint.toPathPoint() }
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