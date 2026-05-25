package com.kylecorry.trail_sense.tools.ai_assistant.domain

data class AiToolKnowledgeEntry(
    val toolId: Long,
    val needs: String,
    val where: String,
    val how: String,
    val values: String,
    val caveats: String,
    val related: String?
)
