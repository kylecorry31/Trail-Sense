package com.kylecorry.trail_sense.shared.text.search

import com.kylecorry.sol.math.statistics.Statistics

class AggregateSearchStrategy(
    private vararg val strategies: Pair<SearchStrategy, Float>
) : SearchStrategy {
    override fun getSearchScore(
        query: String,
        item: SearchItem
    ): Float {
        val results = strategies.map {
            it.first.getSearchScore(query, item) to it.second
        }
        return Statistics.weightedMean(results)
    }
}