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
    fun `buildUserPrompt without context only includes question`() {
        val prompt = AiPromptBuilder.buildUserPrompt(null, "Hello")
        assertTrue(prompt.contains("Hello"))
        assertFalse(prompt.contains("Context"))
    }
}
