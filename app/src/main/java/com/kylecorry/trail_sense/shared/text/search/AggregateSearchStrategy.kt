package com.kylecorry.trail_sense.shared.text.search

class AggregateSearchStrategy(
    private vararg val strategies: Pair<SearchStrategy, Float>
) : SearchStrategy {
    override fun getSearchScore(
        query: String,
        item: SearchItem
    ): Float {
        val totalWeight = strategies.sumOf { it.second.toDouble() }.toFloat()
        if (totalWeight == 0f) return 0f
        val weightedSum = strategies.sumOf {
            (it.first.getSearchScore(query, item) * it.second).toDouble()
        }.toFloat()
        return weightedSum / totalWeight
    }
}