package com.kylecorry.trail_sense.navigation.infrastructure

import androidx.lifecycle.LiveData
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.database.ICleanable
import com.kylecorry.trail_sense.shared.paths.Path
import com.kylecorry.trail_sense.shared.paths.PathPoint
import java.time.Instant

interface IPathService : ICleanable {

    suspend fun addBacktrackPoint(point: PathPoint)
    suspend fun endBacktrackPath()
    suspend fun getBacktrackPathId(): Long?

    fun getLivePaths(): LiveData<List<Path>>
    suspend fun getPath(id: Long): Path?
    fun getLivePath(id: Long): LiveData<Path?>
    suspend fun addPath(path: Path): Long
    suspend fun deletePath(path: Path)

    suspend fun getWaypoints(paths: List<Long>? = null): Map<Long, List<PathPoint>>
    suspend fun getWaypoints(path: Long): List<PathPoint>
    fun getWaypointsLive(path: Long): LiveData<List<PathPoint>>
    suspend fun addWaypoint(point: PathPoint): Long
    suspend fun deleteWaypoint(point: PathPoint)
    suspend fun moveWaypointsToPath(points: List<PathPoint>, pathId: Long)


    fun getRecentAltitudes(since: Instant): LiveData<List<Reading<Float>>>
}