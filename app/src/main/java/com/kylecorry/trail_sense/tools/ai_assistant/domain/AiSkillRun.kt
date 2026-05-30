package com.kylecorry.trail_sense.tools.ai_assistant.domain

data class AiSkillRun(
    val skillId: String,
    val skillName: String,
    val results: List<AiToolRunResult>,
    val interpretationPrompt: String
) {
    fun toCards(): List<AiToolCallCard> {
        return results.map { it.toCard(skillId, skillName) }
    }

    fun toPromptContext(): String {
        return buildString {
            append("Trail Sense skill run: ")
            append(skillName)
            append("\nSkill id: ")
            append(skillId)
            append("\nTool results:")
            results.forEach { result ->
                append("\n- ")
                append(result.toolName)
                append(" [")
                append(result.status.id)
                append("]: ")
                append(result.summary)
                if (result.sensorData.isNotEmpty()) {
                    append("\n  Data: ")
                    append(result.sensorData.entries.joinToString("; ") { "${it.key}=${it.value}" })
                }
                if (!result.error.isNullOrBlank()) {
                    append("\n  Error: ")
                    append(result.error)
                }
            }
            append("\nInterpretation requirements: ")
            append(interpretationPrompt)
        }
    }
}
