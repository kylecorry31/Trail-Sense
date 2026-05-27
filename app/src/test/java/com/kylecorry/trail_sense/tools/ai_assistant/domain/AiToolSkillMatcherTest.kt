package com.kylecorry.trail_sense.tools.ai_assistant.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AiToolSkillMatcherTest {

    @Test
    fun `rank selects avalanche workflow for Chinese avalanche question`() {
        val ranked = AiToolSkillMatcher.rank(
            question = "我现在是否有雪崩风险",
            entries = listOf(
                entry("storm_check", "storm, lightning, 雷暴"),
                entry("avalanche_risk_check", "avalanche, snow slope, 雪崩, 雪坡, 坡度")
            )
        )

        assertEquals("avalanche_risk_check", ranked.first().id)
    }

    @Test
    fun `rank selects navigation workflow for route back question`() {
        val ranked = AiToolSkillMatcher.rank(
            question = "Which tools help me get back to camp?",
            entries = listOf(
                entry("storm_check", "storm, lightning"),
                entry("navigate_back_safely", "get back, return to camp, route back, navigation")
            )
        )

        assertEquals("navigate_back_safely", ranked.first().id)
    }

    private fun entry(id: String, needs: String): AiToolSkillEntry {
        return AiToolSkillEntry(
            id = id,
            name = id,
            needs = needs,
            summary = "Summary",
            toolIds = listOf(11L),
            steps = "Steps",
            interpretation = null,
            caveats = "Caveats",
            samplePrompts = emptyList()
        )
    }
}
