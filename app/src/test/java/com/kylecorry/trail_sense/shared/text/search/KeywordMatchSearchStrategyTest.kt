package com.kylecorry.trail_sense.shared.text.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class KeywordMatchSearchStrategyTest {

    @ParameterizedTest
    @CsvSource(
        "rain, 1.0",
        "RAIN, 1.0",
        "ai, 1.0",
        "snow, 0.0",
        "'', 1.0"
    )
    fun containsMatchStrategy(query: String, expected: Float) {
        val strategy = KeywordMatchSearchStrategy(TextMatchStrategy.Contains)
        val item = SearchItem("1", "title", keywords = setOf("rain", "wind"))
        assertEquals(expected, strategy.getSearchScore(query, item), 0.001f)
    }

    @ParameterizedTest
    @CsvSource(
        "rain, 1.0",
        "RAIN, 1.0",
        "ra, 1.0",
        "ain, 0.0",
        "snow, 0.0"
    )
    fun startsWithMatchStrategy(query: String, expected: Float) {
        val strategy = KeywordMatchSearchStrategy(TextMatchStrategy.StartsWith)
        val item = SearchItem("1", "title", keywords = setOf("rain", "wind"))
        assertEquals(expected, strategy.getSearchScore(query, item), 0.001f)
    }

    @ParameterizedTest
    @CsvSource(
        "rain, 1.0",
        "RAIN, 1.0",
        "ai, 0.0",
        "snow, 0.0"
    )
    fun equalsMatchStrategy(query: String, expected: Float) {
        val strategy = KeywordMatchSearchStrategy(TextMatchStrategy.Equals)
        val item = SearchItem("1", "title", keywords = setOf("rain", "wind"))
        assertEquals(expected, strategy.getSearchScore(query, item), 0.001f)
    }

    @Test
    fun noKeywordsReturnsZero() {
        val strategy = KeywordMatchSearchStrategy()
        val item = SearchItem("1", "title", keywords = emptySet())
        assertEquals(0f, strategy.getSearchScore("rain", item), 0.001f)
    }
}
