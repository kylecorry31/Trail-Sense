package com.kylecorry.trail_sense.tools.ai_assistant.domain

data class AiToolRunResult(
    val toolId: Long,
    val toolName: String,
    val status: AiToolRunStatus,
    val sensorData: Map<String, Any?> = emptyMap(),
    val summary: String,
    val details: String? = null,
    val error: String? = null,
    val openedNavAction: Int? = null,
    val actionLabel: String? = null,
    val actionArguments: Map<String, String> = emptyMap()
) {
    fun toCard(skillId: String, skillName: String? = null): AiToolCallCard {
        val errorDetails = error?.takeIf { it != summary }
        return AiToolCallCard(
            toolId = toolId,
            toolName = toolName,
            skillId = skillId,
            skillName = skillName,
            status = status,
            summary = summary,
            details = errorDetails ?: details ?: sensorData.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                .takeIf { it.isNotBlank() },
            openedNavAction = openedNavAction,
            actionLabel = actionLabel,
            actionArguments = actionArguments,
            timestamp = System.currentTimeMillis()
        )
    }
}
