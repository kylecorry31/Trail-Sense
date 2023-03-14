package com.kylecorry.trail_sense.navigation.paths.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.navigation.paths.domain.*
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import java.time.Instant

class PathLoader(private val pathService: IPathService) {

    val points: MutableMap<Long, List<PathPoint>> = mutableMapOf()

    suspend fun getPointsWithBacktrack(context: Context): Map<Long, List<PathPoint>> = onIO {
        val locationSubsystem = LocationSubsystem.getInstance(context)
        val location = locationSubsystem.location
        val altitude = locationSubsystem.elevation

        val isTracking = BacktrackScheduler.isOn(context)
        val currentBacktrackPathId = pathService.getBacktrackPathId() ?: return@onIO points

        val point = PathPoint(
            -1,
            currentBacktrackPathId,
            location,
            altitude.distance,
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

    suspend fun update(
        paths: List<Path>,
        load: CoordinateBounds,
        unload: CoordinateBounds,
        reload: Boolean = false
    ) = onIO {
        val shouldLoad = ShouldLoadPathSpecification(load)
        val shouldUnload = ShouldUnloadPathSpecification(unload)

        val toLoad = mutableListOf<Long>()

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