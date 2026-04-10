package com.kylecorry.trail_sense.shared.text.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AggregateSearchStrategyTest {

    @Test
    fun returnsWeightedMeanOfStrategies() {
        val always1 = SearchStrategy { _, _ -> 1f }
        val always0 = SearchStrategy { _, _ -> 0f }
        val strategy = AggregateSearchStrategy(always1 to 1f, always0 to 1f)
        val item = SearchItem("1", "title")
        assertEquals(0.5f, strategy.getSearchScore("query", item), 0.001f)
    }

    @Test
    fun respectsWeights() {
        val always1 = SearchStrategy { _, _ -> 1f }
        val always0 = SearchStrategy { _, _ -> 0f }
        val strategy = AggregateSearchStrategy(always1 to 3f, always0 to 1f)
        val item = SearchItem("1", "title")
        assertEquals(0.75f, strategy.getSearchScore("query", item), 0.001f)
    }

    @Test
    fun singleStrategyReturnsItsScore() {
        val always0_8 = SearchStrategy { _, _ -> 0.8f }
        val strategy = AggregateSearchStrategy(always0_8 to 1f)
        val item = SearchItem("1", "title")
        assertEquals(0.8f, strategy.getSearchScore("query", item), 0.001f)
    }
}
