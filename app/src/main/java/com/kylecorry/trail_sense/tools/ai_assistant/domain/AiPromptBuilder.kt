package com.kylecorry.trail_sense.tools.ai_assistant.domain

import java.util.Locale

object AiPromptBuilder {

    fun buildSystemPrompt(locale: Locale): String {
        return """
            You are a wilderness survival assistant built into the Trail Sense app.

            Rules:
            - Respond in the user's language (${locale.language}).
            - Be concise: max 80 words per response.
            - Prioritize safety-critical information first.
            - When interpreting sensor data, explain what it means in practical terms.
            - Never fabricate sensor readings — use only the data provided.
            - When an image is provided, describe what you observe and provide relevant safety or identification information.
            - For cloud images, identify the cloud type and its weather implications.
            - For plant or animal images, attempt identification and note any safety concerns.
            - Always clarify that your advice is supplementary, not a replacement for proper training and judgment.
        """.trimIndent()
    }

    fun buildUserPrompt(context: AiContext?, question: String): String {
        if (context == null) {
            return question
        }
        return buildString {
            append("[Context from ${context.toolName} tool]\n")
            append(context.summary)
            append("\n")
            append("User question: $question")
        }
    }
}
