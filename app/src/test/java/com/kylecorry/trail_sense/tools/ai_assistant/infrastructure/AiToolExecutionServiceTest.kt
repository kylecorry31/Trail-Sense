package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure

import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolRunResult
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolRunStatus
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolSkillEntry
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightNavigationArgs
import com.kylecorry.trail_sense.tools.whistle.domain.WhistleNavigationArgs
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiToolExecutionServiceTest {

    @Test
    fun `execute runs only tools from matched enabled skill`() = runBlocking {
        val runner = FakeRunner()
        val service = AiToolExecutionService(
            skills = listOf(
                entry("storm_check", "storm, lightning", listOf(20, 23, 24)),
                entry("navigate_back_safely", "return to camp, route back", listOf(6, 7))
            ),
            runner = runner
        )

        val run = service.execute(
            question = "Is a storm coming?",
            enabledSkillIds = setOf("storm_check")
        )

        assertEquals("storm_check", run?.skillId)
        assertEquals(listOf(20L, 23L, 24L), runner.runToolIds)
        assertEquals(listOf(20L, 23L, 24L), run?.results?.map { it.toolId })
    }

    @Test
    fun `runTool blocks tools outside active skill allowlist`() = runBlocking {
        val runner = FakeRunner()
        val service = AiToolExecutionService(
            skills = listOf(entry("storm_check", "storm", listOf(20))),
            runner = runner
        )

        service.activateSkill(entry("storm_check", "storm", listOf(20)))
        val result = service.runTool(7)

        assertEquals(AiToolRunStatus.Unavailable, result.status)
        assertEquals(emptyList<Long>(), runner.runToolIds)
    }

    @Test
    fun `executeStepByStep emits running and completed cards around each tool`() = runBlocking {
        val runner = FakeRunner()
        val skill = entry("navigate_back_safely", "return to camp, route back", listOf(6, 7))
        val service = AiToolExecutionService(
            skills = listOf(skill),
            runner = runner
        )
        val events = mutableListOf<String>()

        val run = service.executeStepByStep(
            skill = skill,
            onToolStarted = {
                events.add("start:${it.toolId}:${it.status.id}")
            },
            onToolFinished = {
                events.add("finish:${it.toolId}:${it.status.id}")
            }
        )

        assertEquals(listOf(6L, 7L), runner.runToolIds)
        assertEquals(listOf(6L, 7L), run.results.map { it.toolId })
        assertEquals(
            listOf(
                "start:6:running",
                "finish:6:succeeded",
                "start:7:running",
                "finish:7:succeeded"
            ),
            events
        )
    }

    @Test
    fun `selectSkill ignores weak incidental Chinese matches`() {
        val service = AiToolExecutionService(
            skills = listOf(
                entry(
                    "avalanche_risk_check",
                    "avalanche, snow slope, snowpack, 雪崩, 坡度",
                    listOf(11, 20)
                ),
                entry("storm_check", "storm, lightning", listOf(20, 24))
            ),
            runner = FakeRunner()
        )

        val skill = service.selectSkill("现在海拔是多少")

        assertEquals(null, skill)
    }

    @Test
    fun `execute passes emergency signal action arguments to tools`() = runBlocking {
        val runner = FakeRunner()
        val service = AiToolExecutionService(
            skills = listOf(entry("emergency_signal", "求救, 手电筒, 频率, 哨子", listOf(2, 1))),
            runner = runner
        )

        service.execute(
            question = "我要用手电筒 5Hz 求救",
            enabledSkillIds = setOf("emergency_signal")
        )

        assertJsonField(
            runner.runArgumentsJson[0],
            WhistleNavigationArgs.SIGNAL,
            WhistleNavigationArgs.SIGNAL_HELP
        )
        assertJsonField(
            runner.runArgumentsJson[1],
            FlashlightNavigationArgs.MODE,
            FlashlightNavigationArgs.MODE_STROBE
        )
        assertJsonField(runner.runArgumentsJson[1], FlashlightNavigationArgs.FREQUENCY_HZ, "5")
    }

    private fun assertJsonField(json: String, field: String, value: String) {
        val quotedValue = Regex.escape("\"$value\"")
        val rawValue = Regex.escape(value)
        val pattern = Regex("\"${Regex.escape(field)}\"\\s*:\\s*($quotedValue|$rawValue)")
        assertTrue(pattern.containsMatchIn(json))
    }

    private fun entry(id: String, needs: String, toolIds: List<Long>): AiToolSkillEntry {
        return AiToolSkillEntry(
            id = id,
            name = id,
            needs = needs,
            summary = "Summary",
            toolIds = toolIds,
            steps = "Steps",
            interpretation = "Interpretation",
            caveats = "Caveats",
            samplePrompts = listOf("我现在是否有雪崩风险？")
        )
    }

    private class FakeRunner : AiTrailSenseToolRunner {
        val runToolIds = mutableListOf<Long>()
        val runArgumentsJson = mutableListOf<String>()

        override fun getToolName(toolId: Long): String {
            return "Tool $toolId"
        }

        override fun getOpenToolAction(toolId: Long): Int? {
            return toolId.toInt()
        }

        override suspend fun run(toolId: Long, argumentsJson: String): AiToolRunResult {
            runToolIds.add(toolId)
            runArgumentsJson.add(argumentsJson)
            return AiToolRunResult(
                toolId = toolId,
                toolName = getToolName(toolId),
                status = AiToolRunStatus.Succeeded,
                sensorData = mapOf("tool_id" to toolId),
                summary = "Ran tool $toolId",
                openedNavAction = getOpenToolAction(toolId)
            )
        }
    }
}
