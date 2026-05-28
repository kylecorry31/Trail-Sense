package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolRunResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class AiAssistantTools(
    private val executionService: AiToolExecutionService
) : ToolSet {

    fun configure(enabledSkillIds: Set<String>?) {
        executionService.configure(enabledSkillIds)
    }

    @Tool(description = "Loads a built-in Trail Sense skill workflow before running Trail Sense tools.")
    fun loadSkill(
        @ToolParam(description = "The Trail Sense skill name or id to load.") skillName: String
    ): Map<String, String> {
        val skill = executionService.activateSkill(skillName)
        return if (skill == null) {
            mapOf(
                "status" to "unavailable",
                "skill_name" to skillName,
                "summary" to "Skill not found or not enabled."
            )
        } else {
            mapOf(
                "status" to "succeeded",
                "skill_id" to skill.id,
                "skill_name" to skill.name,
                "tool_ids" to skill.toolIds.joinToString(","),
                "summary" to skill.summary
            )
        }
    }

    @Tool(description = "Runs a read-only Trail Sense tool collector and returns current readings, saved data, or an unavailable reason.")
    fun runTrailSenseTool(
        @ToolParam(description = "The numeric Trail Sense tool id to run.") toolId: String,
        @ToolParam(description = "JSON arguments for the tool call, such as target_elevation_m.") argumentsJson: String
    ): Map<String, String> {
        return runBlocking(Dispatchers.IO) {
            executionService.runTool(toolId, argumentsJson).toToolMap()
        }
    }

    private fun AiToolRunResult.toToolMap(): Map<String, String> {
        val data = JSONObject()
        sensorData.forEach { (key, value) ->
            data.put(key, value)
        }

        return mapOf(
            "tool_id" to toolId.toString(),
            "tool_name" to toolName,
            "status" to status.id,
            "summary" to summary,
            "data" to data.toString(),
            "error" to (error ?: "")
        )
    }
}
