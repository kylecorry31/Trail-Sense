package com.kylecorry.trail_sense.shared.text.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TitleMatchSearchStrategyTest {

    @ParameterizedTest
    @CsvSource(
        "weather, 1.0",
        "WEATHER, 1.0",
        "eath, 1.0",
        "rain, 0.0",
        "'', 1.0"
    )
    fun containsMatchStrategy(query: String, expected: Float) {
        val strategy = TitleMatchSearchStrategy(TextMatchStrategy.Contains)
        val item = SearchItem("1", "Weather Forecast")
        assertEquals(expected, strategy.getSearchScore(query, item), 0.001f)
    }

    @ParameterizedTest
    @CsvSource(
        "weather, 1.0",
        "WEATHER, 1.0",
        "weat, 1.0",
        "forecast, 0.0",
        "rain, 0.0"
    )
    fun startsWithMatchStrategy(query: String, expected: Float) {
        val strategy = TitleMatchSearchStrategy(TextMatchStrategy.StartsWith)
        val item = SearchItem("1", "Weather Forecast")
        assertEquals(expected, strategy.getSearchScore(query, item), 0.001f)
    }

    @ParameterizedTest
    @CsvSource(
        "Weather Forecast, 1.0",
        "WEATHER FORECAST, 1.0",
        "Weather, 0.0",
        "weather forecast extra, 0.0"
    )
    fun equalsMatchStrategy(query: String, expected: Float) {
        val strategy = TitleMatchSearchStrategy(TextMatchStrategy.Equals)
        val item = SearchItem("1", "Weather Forecast")
        assertEquals(expected, strategy.getSearchScore(query, item), 0.001f)
    }
}
