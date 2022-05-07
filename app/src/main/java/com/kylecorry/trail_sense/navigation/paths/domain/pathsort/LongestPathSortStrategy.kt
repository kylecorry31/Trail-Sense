package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.pathsort.provider.PathValueProvider

class LongestPathSortStrategy(
    private val pathService: IPathService
) : AggregationPathSortStrategy<Float>(ascending = false) {
    override fun getProvider(): PathValueProvider<Float> {
        return PathValueProvider(
            pathService,
            { it.metadata.distance.meters().distance }) {
            it.maxOrNull() ?: 0f
        }
    }
}