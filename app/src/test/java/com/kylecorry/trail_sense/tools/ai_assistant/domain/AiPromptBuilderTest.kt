package com.kylecorry.trail_sense.tools.ai_assistant.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Locale

class AiPromptBuilderTest {

    @Test
    fun `buildSystemPrompt includes locale`() {
        val prompt = AiPromptBuilder.buildSystemPrompt(Locale.CHINESE)
        assertTrue(prompt.contains("zh"))
    }

    @Test
    fun `buildSystemPrompt includes safety instructions`() {
        val prompt = AiPromptBuilder.buildSystemPrompt(Locale.ENGLISH)
        assertTrue(prompt.contains("safety"))
        assertTrue(prompt.contains("supplementary"))
        assertTrue(prompt.contains("plain text"))
    }

    @Test
    fun `buildSystemPrompt instructs use of tool knowledge`() {
        val prompt = AiPromptBuilder.buildSystemPrompt(Locale.ENGLISH)
        assertTrue(prompt.contains("Trail Sense tool knowledge"))
        assertTrue(prompt.contains("User Guide"))
    }

    @Test
    fun `buildUserPrompt includes context summary and question`() {
        val context = AiContext(
            toolId = "weather",
            toolName = "Weather",
            sensorData = mapOf("pressure_hpa" to 1008.2f),
            image = null,
            summary = "Pressure: 1008.2 hPa"
        )
        val prompt = AiPromptBuilder.buildUserPrompt(context, "Is it safe?")
        assertTrue(prompt.contains("Pressure: 1008.2 hPa"))
        assertTrue(prompt.contains("Is it safe?"))
    }

    @Test
    fun `buildUserPrompt includes tool knowledge`() {
        val prompt = AiPromptBuilder.buildUserPrompt(
            null,
            "How do I measure slope?",
            "Tool: Clinometer\nHow: Hold the phone against the slope."
        )
        assertTrue(prompt.contains("[Trail Sense tool knowledge]"))
        assertTrue(prompt.contains("Tool: Clinometer"))
        assertTrue(prompt.contains("How do I measure slope?"))
        assertFalse(prompt.contains("Context"))
    }

    @Test
    fun `buildUserPrompt includes recent chat history`() {
        val prompt = AiPromptBuilder.buildUserPrompt(
            null,
            "What about rain?",
            chatHistory = "User: What does pressure mean?\nAssistant: Falling pressure can mean worse weather."
        )
        assertTrue(prompt.contains("[Recent chat history]"))
        assertTrue(prompt.contains("Falling pressure"))
        assertTrue(prompt.contains("What about rain?"))
    }

    @Test
    fun `buildUserPrompt without context only includes question`() {
        val prompt = AiPromptBuilder.buildUserPrompt(null, "Hello")
        assertTrue(prompt.contains("Hello"))
        assertFalse(prompt.contains("Context"))
    }
}
