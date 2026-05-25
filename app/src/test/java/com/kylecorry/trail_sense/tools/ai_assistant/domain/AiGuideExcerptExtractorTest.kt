package com.kylecorry.trail_sense.tools.ai_assistant.domain

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiGuideExcerptExtractorTest {

    @Test
    fun `extract selects pressure section for pressure trend query`() {
        val excerpt = AiGuideExcerptExtractor.extract(
            "pressure trend",
            """
                The Weather tool predicts weather.
                
                ## Temperature
                Temperature shows current and historical values.
                
                ## Pressure
                The pressure chart shows pressure tendency and pressure change over time.
                Falling pressure may indicate storms.
            """.trimIndent()
        )

        assertTrue(excerpt.contains("Weather tool"))
        assertTrue(excerpt.contains("## Pressure"))
        assertTrue(excerpt.contains("pressure tendency"))
        assertFalse(excerpt.contains("## Temperature"))
    }

    @Test
    fun `extract selects slope section for measure slope query`() {
        val excerpt = AiGuideExcerptExtractor.extract(
            "measure slope",
            """
                The Clinometer tool measures angles.
                
                ## Estimate the height of a tree
                Enter the distance to the tree.
                
                ## Measure a slope
                Hold the phone against the slope to measure its angle.
            """.trimIndent()
        )

        assertTrue(excerpt.contains("Clinometer tool"))
        assertTrue(excerpt.contains("## Measure a slope"))
        assertFalse(excerpt.contains("## Estimate the height"))
    }
}
