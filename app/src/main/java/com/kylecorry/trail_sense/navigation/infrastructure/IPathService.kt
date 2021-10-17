package com.kylecorry.trail_sense.navigation.infrastructure

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.shared.database.ICleanable
import com.kylecorry.trail_sense.shared.paths.Path2
import com.kylecorry.trail_sense.shared.paths.PathPoint

interface IPathService : ICleanable {

    suspend fun addBacktrackPoint(point: PathPoint)
    suspend fun endBacktrackPath()
    suspend fun getBacktrackPathId(): Long?

    fun getLivePaths(): LiveData<List<Path2>>
    suspend fun getPath(id: Long): Path2?
    suspend fun addPath(path: Path2): Long
    suspend fun deletePath(path: Path2)

    suspend fun getWaypoints(paths: List<Long>): Map<Long, List<PathPoint>>
    fun getLiveWaypoints(path: Long): LiveData<List<PathPoint>>
    suspend fun addWaypoint(point: PathPoint): Long
    suspend fun deleteWaypoint(point: PathPoint)
}