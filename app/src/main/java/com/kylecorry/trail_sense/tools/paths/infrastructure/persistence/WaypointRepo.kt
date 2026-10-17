package com.kylecorry.trail_sense.tools.paths.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.WaypointEntity
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

    override suspend fun getAll(): List<PathPoint> {
        return waypointDao.getAllSync().map { it.toPathPoint() }
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

    override suspend fun getAllInPaths(pathIds: List<Long>): List<PathPoint> = onIO {
        val points = mutableListOf<WaypointEntity>()
        for (pathId in pathIds) {
            points.addAll(waypointDao.getAllInPathSync(pathId))
        }
        points.map { it.toPathPoint() }
    }

    override fun getAllInPathLive(pathId: Long): LiveData<List<PathPoint>> {
        return waypointDao.getAllInPath(pathId).map {
            it.map { waypoint -> waypoint.toPathPoint() }
        }
    }

    override suspend fun getFirstInPath(pathId: Long): PathPoint? = onIO {
        waypointDao.getFirstInPath(pathId)?.toPathPoint()
    }

    override suspend fun getLastInPath(pathId: Long): PathPoint? = onIO {
        waypointDao.getLastInPath(pathId)?.toPathPoint()
    }

    override suspend fun getCountInPath(pathId: Long): Int = onIO {
        waypointDao.getCountInPath(pathId)
    }

    override suspend fun getMostRecentPathIdWithElevationChange(minChange: Float): Long? = onIO {
        waypointDao.getMostRecentPathIdWithElevationChange(minChange)
    }

    override suspend fun getAllWithCellSignal(): List<PathPoint> = onIO {
        waypointDao.getAllWithCellSignal().map { it.toPathPoint() }
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
