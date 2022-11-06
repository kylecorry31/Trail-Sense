package com.kylecorry.trail_sense.navigation.paths.domain

import androidx.lifecycle.LiveData
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.database.ICleanable
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import java.time.Instant

interface IPathService : ICleanable {

    suspend fun addBacktrackPoint(point: PathPoint)
    suspend fun endBacktrackPath()
    suspend fun getBacktrackPathId(): Long?

    fun loader(): IGroupLoader<IPath>
    fun getLivePaths(): LiveData<List<Path>>
    suspend fun getPath(id: Long): Path?
    suspend fun getGroup(id: Long?): PathGroup?
    suspend fun getPaths(
        groupId: Long?,
        includeGroups: Boolean = true,
        maxDepth: Int? = 1,
        includeRoot: Boolean = false
    ): List<IPath>

    fun getLivePath(id: Long): LiveData<Path?>
    suspend fun addPath(path: Path): Long
    suspend fun addGroup(group: PathGroup): Long
    suspend fun deletePath(path: Path)
    suspend fun deleteGroup(group: PathGroup)
    suspend fun mergePaths(startPathId: Long, endPathId: Long): Long
    suspend fun simplifyPath(path: Long, quality: PathSimplificationQuality): Int

    suspend fun getWaypoints(paths: List<Long>? = null): Map<Long, List<PathPoint>>
    suspend fun getWaypoints(path: Long): List<PathPoint>
    fun getWaypointsLive(path: Long): LiveData<List<PathPoint>>
    suspend fun addWaypointsToPath(points: List<PathPoint>, pathId: Long)
    suspend fun addWaypoint(point: PathPoint): Long
    suspend fun deleteWaypoint(point: PathPoint)
    suspend fun moveWaypointsToPath(points: List<PathPoint>, pathId: Long)

    fun getRecentAltitudesLive(since: Instant): LiveData<List<Reading<Float>>>
    suspend fun getRecentAltitudes(since: Instant): List<Reading<Float>>
}