package com.kylecorry.trail_sense.tools.ai_assistant.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AiToolKnowledgeMatcherTest {

    @Test
    fun `rank selects clinometer for Chinese slope question even when navigation is preferred`() {
        val entries = listOf(
            entry(
                id = 6,
                needs = "导航, 指南针, 方位角, 海拔, 速度"
            ),
            entry(
                id = 11,
                needs = "坡度, 倾角, 斜坡, 坡面, 测角"
            )
        )

        val ranked = AiToolKnowledgeMatcher.rank(
            question = "Trail Sense 里怎么测坡度",
            entries = entries,
            preferredToolId = 6,
            limit = 2
        )

        assertEquals(listOf(11L), ranked)
    }

    @Test
    fun `rank boosts preferred tool only when it is relevant`() {
        val entries = listOf(
            entry(
                id = 6,
                needs = "navigation, compass, bearing, 导航, 指南针"
            ),
            entry(
                id = 11,
                needs = "slope, angle, 坡度, 倾角"
            )
        )

        val ranked = AiToolKnowledgeMatcher.rank(
            question = "怎么使用导航",
            entries = entries,
            preferredToolId = 6,
            limit = 2
        )

        assertEquals(6L, ranked.first())
    }

    private fun entry(id: Long, needs: String): AiToolKnowledgeEntry {
        return AiToolKnowledgeEntry(
            toolId = id,
            needs = needs,
            where = "Open from Tools.",
            how = "Use it.",
            values = "Values explain the reading.",
            caveats = "Use caution.",
            related = null
        )
    }
}
