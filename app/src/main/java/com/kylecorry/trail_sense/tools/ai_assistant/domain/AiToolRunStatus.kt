package com.kylecorry.trail_sense.tools.ai_assistant.domain

enum class AiToolRunStatus(val id: String) {
    Running("running"),
    Succeeded("succeeded"),
    Unavailable("unavailable"),
    Failed("failed");

    companion object {
        fun from(id: String?): AiToolRunStatus {
            return values().firstOrNull { it.id == id } ?: Failed
        }
    }
}
