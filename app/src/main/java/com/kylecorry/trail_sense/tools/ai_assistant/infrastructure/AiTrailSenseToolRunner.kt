package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure

import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolRunResult

interface AiTrailSenseToolRunner {
    fun getToolName(toolId: Long): String
    fun getOpenToolAction(toolId: Long): Int?
    suspend fun run(toolId: Long, argumentsJson: String = "{}"): AiToolRunResult
}
