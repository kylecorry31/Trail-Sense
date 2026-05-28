package com.kylecorry.trail_sense.tools.ai_assistant.domain

data class AiToolRunResult(
    val toolId: Long,
    val toolName: String,
    val status: AiToolRunStatus,
    val sensorData: Map<String, Any?> = emptyMap(),
    val summary: String,
    val error: String? = null,
    val openedNavAction: Int? = null
) {
    fun toCard(skillId: String): AiToolCallCard {
        return AiToolCallCard(
            toolId = toolId,
            toolName = toolName,
            skillId = skillId,
            status = status,
            summary = summary,
            details = error ?: sensorData.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                .takeIf { it.isNotBlank() },
            openedNavAction = openedNavAction,
            timestamp = System.currentTimeMillis()
        )
    }
}
