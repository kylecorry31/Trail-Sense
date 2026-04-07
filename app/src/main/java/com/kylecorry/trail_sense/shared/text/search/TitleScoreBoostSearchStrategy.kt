package com.kylecorry.trail_sense.shared.text.search

import com.kylecorry.sol.math.MathExtensions.real

class TitleScoreBoostSearchStrategy(
    private val strategy: SearchStrategy,
    private val containsBoost: Float = 1.1f,
    private val startsWithBoost: Float = 1.1f,
    private val containsMinScore: Float = 0.5f,
    private val startsWithMinScore: Float = 0.6f
) : SearchStrategy {

    override fun getSearchScore(query: String, item: SearchItem): Float {
        val score = strategy.getSearchScore(query, item).real(0f)
        val title = item.title
        val boost = when {
            title.startsWith(query, ignoreCase = true) -> startsWithBoost
            title.contains(query, ignoreCase = true) -> containsBoost
            else -> 1f
        }
        val minScore = when {
            title.startsWith(query, ignoreCase = true) -> startsWithMinScore
            title.contains(query, ignoreCase = true) -> containsMinScore
            else -> 0f
        }
        return maxOf(score * boost, minScore)
    }
}
