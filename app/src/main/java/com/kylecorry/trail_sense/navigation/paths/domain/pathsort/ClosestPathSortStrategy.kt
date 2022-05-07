package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.pathsort.provider.PathValueProvider

class ClosestPathSortStrategy(
    private val location: Coordinate,
    private val pathService: IPathService
) : AggregationPathSortStrategy<Float>() {
    override fun getProvider(): PathValueProvider<Float> {
        return PathValueProvider(
            pathService,
            { it.metadata.bounds.center.distanceTo(location) }) {
            it.minOrNull() ?: Float.POSITIVE_INFINITY
        }
    }
}