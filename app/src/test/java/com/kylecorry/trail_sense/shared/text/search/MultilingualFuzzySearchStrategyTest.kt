package com.kylecorry.trail_sense.shared.text.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class MultilingualFuzzySearchStrategyTest {

    private val strategy = MultilingualFuzzySearchStrategy()

    @ParameterizedTest
    @CsvSource(
        "Navigation, Navigation",
        "NAVIGATION, Navigation",
        "导航, 导航"
    )
    fun boostsExactEnglishAndChineseTitleMatches(query: String, title: String) {
        val item = SearchItem("navigation", title)

        assertEquals(1.1f, strategy.getSearchScore(query, item), 0.001f)
    }

    @Test
    fun returnsAnUnboostedScoreForPartialChineseTitleMatches() {
        val item = SearchItem("navigation", "导航 工具")

        assertEquals(1f, strategy.getSearchScore("导航", item), 0.001f)
    }

    @Test
    fun returnsAnExactScoreForKeywordMatchWhenTitleDoesNotMatch() {
        val item = SearchItem(
            id = "navigation",
            title = "Compass",
            keywords = setOf("navigation")
        )

        assertEquals(1f, strategy.getSearchScore("navigation", item), 0.001f)
    }

    @Test
    fun givesAPositiveButUnboostedScoreForFuzzyEnglishMatch() {
        val item = SearchItem("navigation", "Navigation")
        val score = strategy.getSearchScore("navigtion", item)

        assertTrue(score > 0f)
        assertTrue(score < 1f)
    }

    @Test
    fun boostsKeywordScoreWhenParentTitleMatches() {
        val itemWithoutParent = SearchItem(
            id = "compass",
            title = "Compass",
            keywords = setOf("compass")
        )
        val itemWithMatchingParent = SearchItem(
            id = "compass",
            title = "Compass",
            keywords = setOf("compass"),
            parent = SearchItem("navigation", "Navigation")
        )

        val scoreWithoutParent = strategy.getSearchScore("navigation", itemWithoutParent)
        val scoreWithParent = strategy.getSearchScore("navigation", itemWithMatchingParent)

        assertTrue(scoreWithParent > scoreWithoutParent)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "🧭", "---", "   "])
    fun returnsZeroWhenTheTokenizerCannotProduceTokens(text: String) {
        val item = SearchItem("compass", text)
        val score = strategy.getSearchScore(text, item)

        assertFalse(score.isNaN())
        assertEquals(0f, score, 0.001f)
    }
}
