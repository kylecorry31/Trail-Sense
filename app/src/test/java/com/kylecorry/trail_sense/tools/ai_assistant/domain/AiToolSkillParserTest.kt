package com.kylecorry.trail_sense.tools.ai_assistant.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AiToolSkillParserTest {

    @Test
    fun `parse reads complete skill blocks`() {
        val entries = AiToolSkillParser.parse(
            """
                # Skills
                
                ## Avalanche Risk Check
                Skill ID: avalanche_risk_check
                Name zh: 雪崩风险检查
                Needs: avalanche, slope, 雪崩
                Summary: Combine tools for avalanche terrain clues.
                Summary zh: 组合多个工具检查雪崩线索。
                Tools: 11, 20, 23
                Steps: Measure slope, then check weather and clouds.
                Steps zh: 先测坡度，再检查天气和云。
                Interpretation: Treat results as concern signals.
                Interpretation zh: 只能作为风险关注信号。
                Caveats: Use official avalanche forecasts.
                Caveats zh: 使用官方雪崩预报。
                Sample prompts: Am I in avalanche terrain? | 我现在是否有雪崩风险？
            """.trimIndent()
        )

        assertEquals(1, entries.size)
        assertEquals("avalanche_risk_check", entries[0].id)
        assertEquals("Avalanche Risk Check", entries[0].name)
        assertEquals("雪崩风险检查", entries[0].name("zh"))
        assertEquals("组合多个工具检查雪崩线索。", entries[0].summary("zh"))
        assertEquals("先测坡度，再检查天气和云。", entries[0].steps("zh"))
        assertEquals("Treat results as concern signals.", entries[0].interpretation("en"))
        assertEquals("只能作为风险关注信号。", entries[0].interpretation("zh"))
        assertEquals("使用官方雪崩预报。", entries[0].caveats("zh"))
        assertEquals(listOf(11L, 20L, 23L), entries[0].toolIds)
        assertEquals(2, entries[0].samplePrompts.size)
    }

    @Test
    fun `parse ignores malformed skill blocks`() {
        val entries = AiToolSkillParser.parse(
            """
                ## Broken
                Skill ID: broken
                Needs: avalanche
                
                ## Complete
                Skill ID: storm_check
                Needs: storm
                Summary: Check storm clues.
                Tools: 20
                Steps: Check pressure.
                Caveats: Seek shelter.
            """.trimIndent()
        )

        assertEquals(1, entries.size)
        assertEquals("storm_check", entries[0].id)
    }
}
