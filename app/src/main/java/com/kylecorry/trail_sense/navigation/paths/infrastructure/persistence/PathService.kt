package com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kylecorry.andromeda.core.time.ITimeProvider
import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.filters.RDPFilter
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.navigation.paths.domain.*
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.grouping.count.GroupCounter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupDeleter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupLoader
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import kotlin.math.absoluteValue

class PathService(
    private val pathRepo: IPathRepo,
    private val waypointRepo: IWaypointRepo,
    private val pathPreferences: IPathPreferences,
    private val cache: Preferences,
    private val time: ITimeProvider = SystemTimeProvider()
) : IPathService {

    private val backtrackLock = Mutex()
    private val loader = GroupLoader(this::getGroup, this::getChildren)
    private val counter = GroupCounter(loader)
    private val deleter = object : GroupDeleter<IPath>(loader) {
        override suspend fun deleteItems(items: List<IPath>) {
            items.forEach { deletePath(it as Path) }
        }

        override suspend fun deleteGroup(group: IPath) {
            pathRepo.deleteGroup(group as PathGroup)
        }
    }

    override suspend fun addBacktrackPoint(point: PathPoint) {
        backtrackLock.withLock {
            val id = cache.getLong(BACKTRACK_PATH_KEY)
            val backtrackId = if (id == null || getPath(id) == null) {
                val newId = createBacktrackPath()
                cache.putLong(BACKTRACK_PATH_KEY, newId)
                newId
            } else {
                id
            }

            addWaypoint(point.copy(pathId = backtrackId))
        }
    }

    override suspend fun endBacktrackPath() {
        backtrackLock.withLock {
            cache.remove(BACKTRACK_PATH_KEY)
        }
    }

    override suspend fun getBacktrackPathId(): Long? {
        backtrackLock.withLock {
            return cache.getLong(BACKTRACK_PATH_KEY)
        }
    }

    override fun loader(): IGroupLoader<IPath> {
        return loader
    }

    override fun getLivePaths(): LiveData<List<Path>> {
        return pathRepo.getAllLive()
    }

    override suspend fun getPath(id: Long): Path? {
        return pathRepo.get(id)
    }

    override suspend fun getPaths(
        groupId: Long?,
        includeGroups: Boolean,
        maxDepth: Int?,
        includeRoot: Boolean
    ): List<IPath> {
        return onIO {

            val root = listOfNotNull(
                if (includeRoot) {
                    loader.getGroup(groupId)
                } else {
                    null
                }
            )

            val paths = root + loader.getChildren(groupId, maxDepth)
            if (includeGroups) {
                paths
            } else {
                paths.filterNot { it.isGroup }
            }
        }
    }

    override suspend fun getGroup(id: Long?): PathGroup? {
        id ?: return null
        return pathRepo.getGroup(id)?.copy(count = counter.count(id))
    }

    private suspend fun getGroups(parent: Long?): List<PathGroup> {
        return pathRepo.getGroupsWithParent(parent).map { it.copy(count = counter.count(it.id)) }
    }

    private suspend fun getChildren(groupId: Long?): List<IPath> {
        val paths = pathRepo.getPathsWithParent(groupId)
        val groups = getGroups(groupId)
        return paths + groups
    }

    override fun getLivePath(id: Long): LiveData<Path?> {
        return pathRepo.getLive(id)
    }

    override suspend fun addPath(path: Path): Long {
        return pathRepo.add(path)
    }

    override suspend fun addGroup(group: PathGroup): Long {
        return pathRepo.addGroup(group)
    }

    override suspend fun deletePath(path: Path) {
        backtrackLock.withLock {
            val backtrackId = cache.getLong(BACKTRACK_PATH_KEY)
            if (backtrackId == path.id) {
                cache.remove(BACKTRACK_PATH_KEY)
            }
        }

        waypointRepo.deleteInPath(path.id)
        pathRepo.delete(path)
    }

    override suspend fun deleteGroup(group: PathGroup) {
        deleter.delete(group)
    }

    override suspend fun mergePaths(startPathId: Long, endPathId: Long): Long {
        val start = getPath(startPathId) ?: return 0L
        val end = getPath(endPathId) ?: return 0L
        val backtrackId = getBacktrackPathId()
        val wasBacktrack = start.id == backtrackId || end.id == backtrackId
        val style = start.style
        val name = start.name ?: end.name
        val parentId = start.parentId
        val startPoints = getWaypoints(startPathId)
        val endPoints = getWaypoints(endPathId)

        val newPathId = addPath(getEmptyPath(false).copy(name = name, style = style, parentId = parentId))

        val allPoints = (startPoints + endPoints).map {
            it.copy(id = 0)
        }
        addWaypointsToPath(allPoints, newPathId)

        if (wasBacktrack) {
            backtrackLock.withLock {
                cache.putLong(BACKTRACK_PATH_KEY, newPathId)
            }
        }

        deletePath(start)
        deletePath(end)
        return newPathId
    }

    override suspend fun simplifyPath(path: Long, quality: PathSimplificationQuality): Int {
        val epsilon = when (quality) {
            PathSimplificationQuality.Low -> 8f
            PathSimplificationQuality.Medium -> 4f
            PathSimplificationQuality.High -> 2f
        }
        val filter = RDPFilter<PathPoint>(epsilon) { point, start, end ->
            Geology.getCrossTrackDistance(
                point.coordinate,
                start.coordinate,
                end.coordinate
            ).distance.absoluteValue
        }

        val points = getWaypoints(path).sortedBy { it.id }.toMutableList()
        val toKeep = filter.filter(points)

        val numDeleted = points.size - toKeep.size

        points.removeAll(toKeep)

        waypointRepo.deleteAll(points)

        updatePathMetadata(path)
        return numDeleted
    }

    override suspend fun getWaypoints(paths: List<Long>?): Map<Long, List<PathPoint>> {
        if (paths?.isEmpty() == true) {
            return mapOf()
        }

        val points = if (paths != null) {
            waypointRepo.getAllInPaths(paths)
        } else {
            waypointRepo.getAll()
        }
        return points.groupBy { it.pathId }
    }

    override suspend fun getWaypoints(path: Long): List<PathPoint> {
        return waypointRepo.getAllInPaths(listOf(path))
    }

    override fun getWaypointsLive(path: Long): LiveData<List<PathPoint>> {
        return waypointRepo.getAllInPathLive(path)
    }

    override suspend fun addWaypointsToPath(points: List<PathPoint>, pathId: Long) {
        waypointRepo.addAll(points.map { it.copy(pathId = pathId) })
        updatePathMetadata(pathId)
    }

    override suspend fun addWaypoint(point: PathPoint): Long {
        val ret = waypointRepo.add(point)
        updatePathMetadata(point.pathId)
        return ret
    }

    override suspend fun deleteWaypoint(point: PathPoint) {
        waypointRepo.delete(point)
        updatePathMetadata(point.pathId)
    }

    override suspend fun moveWaypointsToPath(points: List<PathPoint>, pathId: Long) {
        val oldPaths =
            points.filter { it.pathId != 0L && it.pathId != pathId }.map { it.pathId }.distinct()
        waypointRepo.addAll(points.map { it.copy(pathId = pathId) })
        updatePathMetadata(pathId)
        for (path in oldPaths) {
            updatePathMetadata(pathId)
        }
    }

    override fun getRecentAltitudes(since: Instant): LiveData<List<Reading<Float>>> {
        val recent = waypointRepo.getAllLive(since)
        return Transformations.map(recent) {
            it.filter { point -> point.elevation != null && point.time != null }
                .map { point -> Reading(point.elevation!!, point.time!!) }
        }
    }

    override suspend fun clean() {
        val paths = pathRepo.getAll().filter { it.temporary }
        for (path in paths) {
            deleteOldWaypoints(path.id)
        }
    }

    private suspend fun deleteOldWaypoints(pathId: Long) {
        waypointRepo.deleteOlderInPath(
            pathId,
            time.getTime().toInstant().minus(pathPreferences.backtrackHistory)
        )
        updatePathMetadata(pathId, true)
    }

    private suspend fun updatePathMetadata(pathId: Long, deleteIfEmpty: Boolean = false) {
        val path = getPath(pathId) ?: return
        val points = waypointRepo.getAllInPaths(listOf(pathId)).sortedBy { it.id }

        if (deleteIfEmpty && points.isEmpty()) {
            deletePath(path)
            return
        }

        val coords = points.map { it.coordinate }
        val start = points.firstOrNull()?.time
        val end = points.lastOrNull()?.time
        val metadata = PathMetadata(
            Geology.getPathDistance(coords),
            points.size,
            if (start != null && end != null) Range(start, end) else null,
            Geology.getBounds(coords)
        )

        addPath(path.copy(metadata = metadata))
    }

    private suspend fun createBacktrackPath(): Long {
        val path = getEmptyPath(true)
        return addPath(path)
    }

    private fun getEmptyPath(temporary: Boolean = false): Path {
        return Path(
            0,
            null,
            pathPreferences.defaultPathStyle,
            PathMetadata.empty,
            temporary = temporary
        )
    }

    companion object {
        private const val BACKTRACK_PATH_KEY = "last_backtrack_path_id"
        private var instance: PathService? = null

        @Synchronized
        fun getInstance(context: Context): PathService {
            if (instance == null) {
                instance = PathService(
                    PathRepo.getInstance(context),
                    WaypointRepo.getInstance(context),
                    NavigationPreferences(context),
                    Preferences(context)
                )
            }
            return instance!!
        }
    }
}