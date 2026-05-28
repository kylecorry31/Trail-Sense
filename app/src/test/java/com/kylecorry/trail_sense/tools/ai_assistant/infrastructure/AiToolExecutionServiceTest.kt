package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure

import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolRunResult
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolRunStatus
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolSkillEntry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
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
            samplePrompts = emptyList()
        )
    }

    private class FakeRunner : AiTrailSenseToolRunner {
        val runToolIds = mutableListOf<Long>()

        override fun getToolName(toolId: Long): String {
            return "Tool $toolId"
        }

        override fun getOpenToolAction(toolId: Long): Int? {
            return toolId.toInt()
        }

        override suspend fun run(toolId: Long, argumentsJson: String): AiToolRunResult {
            runToolIds.add(toolId)
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
