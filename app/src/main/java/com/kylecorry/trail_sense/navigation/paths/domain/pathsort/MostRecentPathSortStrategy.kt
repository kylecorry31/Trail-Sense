package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.pathsort.provider.PathValueProvider

class MostRecentPathSortStrategy(
    private val pathService: IPathService
) : AggregationPathSortStrategy<Long>(ascending = false) {
    override fun getProvider(): PathValueProvider<Long> {
        return PathValueProvider(
            pathService,
            { it.id }) {
            it.maxOrNull() ?: 0
        }
    }
}