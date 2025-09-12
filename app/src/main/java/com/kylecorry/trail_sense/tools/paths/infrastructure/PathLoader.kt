package com.kylecorry.trail_sense.tools.paths.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.ShouldLoadPathSpecification
import com.kylecorry.trail_sense.tools.paths.domain.ShouldUnloadPathSpecification
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

class PathLoader(private val pathService: IPathService) {

    val points: MutableMap<Long, List<PathPoint>> = mutableMapOf()
    val pointsLock = Mutex()

    suspend fun getPointsWithBacktrack(context: Context): Map<Long, List<PathPoint>> = onIO {
        val locationSubsystem = LocationSubsystem.getInstance(context)
        val location = locationSubsystem.location
        val altitude = locationSubsystem.elevation

        val isTracking = BacktrackScheduler.isOn(context)
        val currentBacktrackPathId = pathService.getBacktrackPathId()
        pointsLock.withLock {
            if (currentBacktrackPathId == null){
                return@onIO points.toMap()
            }
            val point = PathPoint(
                -1,
                currentBacktrackPathId,
                location,
                altitude.value,
                Instant.now()
            )

            points.mapValues { entry ->
                if (isTracking && entry.key == currentBacktrackPathId) {
                    listOf(point) + entry.value
                } else {
                    entry.value
                }
            }
        }
    }

    suspend fun update(
        paths: List<Path>,
        load: CoordinateBounds,
        unload: CoordinateBounds,
        reload: Boolean = false
    ) = onIO {
        val shouldLoad = ShouldLoadPathSpecification(load)
        val shouldUnload = ShouldUnloadPathSpecification(unload)

        val toLoad = mutableListOf<Long>()

        pointsLock.withLock {
            for (path in paths) {
                if (!reload && points.containsKey(path.id)) {
                    if (shouldUnload.isSatisfiedBy(path)) {
                        points.remove(path.id)
                    }
                }

                if (reload || !points.containsKey(path.id)) {
                    if (shouldLoad.isSatisfiedBy(path)) {
                        toLoad.add(path.id)
                    }
                }
            }

            val loaded =
                pathService.getWaypoints(toLoad).mapValues { it.value.sortedByDescending { it.id } }
            points.putAll(loaded)
        }
    }

}