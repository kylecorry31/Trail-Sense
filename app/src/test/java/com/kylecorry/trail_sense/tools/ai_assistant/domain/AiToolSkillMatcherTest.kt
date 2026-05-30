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

    @Test
    fun `rank selects storm workflow for storm question`() {
        val ranked = AiToolSkillMatcher.rank(
            question = "Is a storm coming?",
            entries = listOf(
                entry("storm_check", "storm, severe weather, thunder, lightning, falling pressure"),
                entry("cold_elevation_planning", "cold, elevation temperature")
            )
        )

        assertEquals("storm_check", ranked.first().id)
    }

    @Test
    fun `rank selects cold workflow for elevation temperature question`() {
        val ranked = AiToolSkillMatcher.rank(
            question = "How cold will it be at the summit?",
            entries = listOf(
                entry("storm_check", "storm, severe weather"),
                entry("cold_elevation_planning", "cold, elevation temperature, freezing, mountain weather")
            )
        )

        assertEquals("cold_elevation_planning", ranked.first().id)
    }

    @Test
    fun `rank selects emergency signal workflow for Chinese rescue question`() {
        val ranked = AiToolSkillMatcher.rank(
            question = "我要求救",
            entries = listOf(
                entry("storm_check", "storm, lightning, 雷暴"),
                entry("emergency_signal", "求救, 救命, SOS, 哨子, 手电筒, 频闪")
            )
        )

        assertEquals("emergency_signal", ranked.first().id)
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
