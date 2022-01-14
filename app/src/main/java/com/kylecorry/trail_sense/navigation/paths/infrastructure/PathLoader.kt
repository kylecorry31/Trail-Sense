package com.kylecorry.trail_sense.navigation.paths.infrastructure

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.navigation.paths.domain.*

class PathLoader(private val pathService: IPathService) {

    val points: MutableMap<Long, List<PathPoint>> = mutableMapOf()

    suspend fun update(
        paths: List<Path>,
        load: CoordinateBounds,
        unload: CoordinateBounds,
        reload: Boolean = false
    ) {
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