package com.kylecorry.trail_sense.tools.ai_assistant.domain

import java.util.Locale

object AiPromptBuilder {

    fun buildSystemPrompt(locale: Locale): String {
        return """
            You are a wilderness survival assistant built into the Trail Sense app.

            Rules:
            - Respond in the user's language (${locale.language}).
            - Be concise for simple questions, but give enough detail for screenshots, tool data, and sensor value explanations.
            - Use plain text only; do not use Markdown formatting.
            - Prioritize safety-critical information first.
            - When interpreting sensor data, explain what it means in practical terms.
            - Never fabricate sensor readings — use only the data provided.
            - For questions about Trail Sense features, use the provided Trail Sense tool knowledge first.
            - If a Trail Sense skill workflow is provided, use it before individual tool notes.
            - Do not assume the current tool is the answer; recommend the tool that best matches the user's request.
            - When a task needs multiple Trail Sense tools, answer as a workflow: primary tool, supporting tools, order of use, what each tool contributes, and caveats.
            - For workflow answers, include concrete steps for using each recommended tool and explain how to interpret the readings or observations.
            - If no matching tool knowledge is provided, suggest searching the Tools list or User Guide instead of inventing features.
            - When an image is provided, treat it as important context and explicitly refer to what is visible in it.
            - When a new image is provided, identify the screen from that image and the current user question; do not let earlier chat history override the visible screenshot.
            - For Trail Sense screenshots, read visible labels, numbers, units, arrows, and panels; explain what each visible value likely means in the current tool.
            - If a value or label is unclear in the image, say it is unclear instead of guessing.
            - For cloud images, identify the cloud type and its weather implications.
            - For plant or animal images, attempt identification and note any safety concerns.
            - Always clarify that your advice is supplementary, not a replacement for proper training and judgment.
        """.trimIndent()
    }

    fun buildUserPrompt(
        context: AiContext?,
        question: String,
        toolKnowledge: String? = null,
        chatHistory: String? = null,
        hasImage: Boolean = false
    ): String {
        if (context == null && toolKnowledge.isNullOrBlank() && chatHistory.isNullOrBlank() && !hasImage) {
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
            if (hasImage) {
                append("[Attached image]\n")
                append("Use the attached image as visual context. If it is a Trail Sense screenshot, identify the visible tool or screen, read visible labels/numbers/units, and explain the values in practical terms. Do not invent values that are not visible.\n\n")
            }
            append("User question: $question")
        }
    }
}
