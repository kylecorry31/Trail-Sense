package com.kylecorry.trail_sense.tools.ai_assistant.domain

import java.util.Locale

object AiPromptBuilder {

    fun buildSystemPrompt(locale: Locale): String {
        return """
            You are a wilderness survival assistant built into the Trail Sense app.

            Rules:
            - Respond in the user's language (${locale.language}).
            - Be concise: max 80 words per response.
            - Use plain text only; do not use Markdown formatting.
            - Prioritize safety-critical information first.
            - When interpreting sensor data, explain what it means in practical terms.
            - Never fabricate sensor readings — use only the data provided.
            - For questions about Trail Sense features, use the provided Trail Sense tool knowledge first.
            - Do not assume the current tool is the answer; recommend the tool that best matches the user's request.
            - If no matching tool knowledge is provided, suggest searching the Tools list or User Guide instead of inventing features.
            - When an image is provided, describe what you observe and provide relevant safety or identification information.
            - For cloud images, identify the cloud type and its weather implications.
            - For plant or animal images, attempt identification and note any safety concerns.
            - Always clarify that your advice is supplementary, not a replacement for proper training and judgment.
        """.trimIndent()
    }

    fun buildUserPrompt(
        context: AiContext?,
        question: String,
        toolKnowledge: String? = null,
        chatHistory: String? = null
    ): String {
        if (context == null && toolKnowledge.isNullOrBlank() && chatHistory.isNullOrBlank()) {
            return question
        }
        return buildString {
            if (!chatHistory.isNullOrBlank()) {
                append("[Recent chat history]\n")
                append(chatHistory)
                append("\n\n")
            }
            if (!toolKnowledge.isNullOrBlank()) {
                append("[Trail Sense tool knowledge]\n")
                append(toolKnowledge)
                append("\n\n")
            }
            if (context != null) {
                append("[Context from ${context.toolName} tool]\n")
                append(context.summary)
                append("\n\n")
            }
            append("User question: $question")
        }
    }
}
