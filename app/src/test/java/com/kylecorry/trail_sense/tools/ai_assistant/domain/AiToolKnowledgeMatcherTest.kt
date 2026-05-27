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

    @Test
    fun `rank can match localized tool name from extra search text`() {
        val entries = listOf(
            entry(
                id = 6,
                needs = "navigation, compass, bearing, 导航, 指南针"
            ),
            entry(
                id = 11,
                needs = "angle, slope, incline"
            )
        )

        val ranked = AiToolKnowledgeMatcher.rank(
            question = "测斜仪怎么用",
            entries = entries,
            limit = 2
        ) {
            if (it.toolId == 11L) "测斜仪" else ""
        }

        assertEquals(11L, ranked.first())
    }

    @Test
    fun `rank selects local talk for Chinese walkie talkie question`() {
        val entries = listOf(
            entry(
                id = 39,
                needs = "本地消息收发, 本地消息, 离线消息, 发消息, 收消息"
            ),
            entry(
                id = 40,
                needs = "本地交谈, 对讲机, 语音通信, 语音通话, 无线电通话"
            )
        )

        val ranked = AiToolKnowledgeMatcher.rank(
            question = "Trail Sense 里对讲机怎么用",
            entries = entries,
            limit = 2
        )

        assertEquals(40L, ranked.first())
    }

    @Test
    fun `rank selects declination for Chinese declination question`() {
        val entries = listOf(
            entry(
                id = 6,
                needs = "导航, 指南针, 方位角"
            ),
            entry(
                id = 46,
                needs = "磁偏角, 磁偏差, 真北, 磁北, 指南针校正, 磁偏角修正"
            )
        )

        val ranked = AiToolKnowledgeMatcher.rank(
            question = "怎么校正指南针的磁偏角",
            entries = entries,
            limit = 2
        )

        assertEquals(46L, ranked.first())
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
