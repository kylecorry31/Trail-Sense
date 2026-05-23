package com.kylecorry.trail_sense.tools.ai_assistant.domain

interface AiContextProvider {
    val toolId: String
    suspend fun getAiContext(): AiContext
    fun getSuggestedQuestions(): List<String>
}
