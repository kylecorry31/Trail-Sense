package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService

object PathRouteBuilder {
    private val hikingService = HikingService()

    fun buildRoute(
        points: List<PathPoint>,
        location: Coordinate,
        reversed: Boolean,
        destinationPointId: Long? = null,
        elevationSmoothing: Float = HikingService.DEFAULT_ELEVATION_SMOOTHING
    ): List<PathPoint> {
        require(points.isNotEmpty())
        val sorted = hikingService.correctElevations(points.sortedBy { it.id }, elevationSmoothing)

        val strategy = if (destinationPointId != null) {
            ShortestRoutingStrategy(sorted.indexOfFirst { it.id == destinationPointId })
        } else {
            InOrderRoutingStrategy(reversed)
        }
        return strategy.buildRoute(sorted, location)
    }
}
