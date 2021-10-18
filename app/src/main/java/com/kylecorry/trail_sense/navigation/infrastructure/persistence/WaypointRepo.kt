package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.shared.paths.PathPoint
import java.time.Instant

class WaypointRepo(context: Context): IWaypointRepo {
    override suspend fun add(value: PathPoint): Long {
        TODO("Not yet implemented")
    }

    override suspend fun delete(value: PathPoint) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteInPath(pathId: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteOlderInPath(pathId: Long, time: Instant) {
        TODO("Not yet implemented")
    }

    override suspend fun get(id: Long): PathPoint? {
        TODO("Not yet implemented")
    }

    override suspend fun getAll(): List<PathPoint> {
        TODO("Not yet implemented")
    }

    override fun getAllLive(): LiveData<List<PathPoint>> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllInPaths(pathIds: List<Long>): List<PathPoint> {
        TODO("Not yet implemented")
    }

    override fun getAllInPathsLive(pathIds: List<Long>): LiveData<List<PathPoint>> {
        TODO("Not yet implemented")
    }
}