package com.kylecorry.trail_sense.tools.navigation.domain.paths

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService

object PathRouteBuilder {
    private val hikingService = HikingService()

    fun buildRoute(
        points: List<PathPoint>,
        location: Coordinate,
        mode: PathNavigationMode,
        destinationPointId: Long? = null
    ): List<PathPoint> {
        require(points.isNotEmpty())
        val sorted = hikingService.correctElevations(points.sortedBy { it.id })

        val strategy = when {
            destinationPointId != null -> ShortestRoutingStrategy(sorted.indexOfFirst { it.id == destinationPointId })
            mode.isFullLoop -> InOrderRoutingStrategy(mode.isReversed)
            else -> ShortestRoutingStrategy(if (mode.isReversed) 0 else sorted.lastIndex)
        }
        return strategy.buildRoute(sorted, location)
    }
}

