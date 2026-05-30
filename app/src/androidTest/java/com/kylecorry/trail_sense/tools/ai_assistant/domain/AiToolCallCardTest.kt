package com.kylecorry.trail_sense.tools.ai_assistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiToolCallCardTest {

    @Test
    fun serializesAndDeserializesToolCallCards() {
        val cards = listOf(
            AiToolCallCard(
                toolId = 20,
                toolName = "Weather",
                skillId = "storm_check",
                status = AiToolRunStatus.Succeeded,
                summary = "Pressure is falling.",
                details = "pressure_change_hpa: -3.1",
                openedNavAction = 123,
                actionLabel = "Open Weather",
                actionArguments = mapOf("mode" to "details"),
                timestamp = 456
            ),
            AiToolCallCard(
                toolId = 11,
                toolName = "Clinometer",
                skillId = "avalanche_risk_check",
                status = AiToolRunStatus.Unavailable,
                summary = "Slope angle required.",
                timestamp = 789
            )
        )

        val parsed = AiToolCallCard.listFromJson(AiToolCallCard.toJson(cards))

        assertEquals(cards[0], parsed[0])
        assertEquals("Open Weather", parsed[0].actionLabel)
        assertEquals("details", parsed[0].actionArguments["mode"])
        assertEquals(cards[1].toolId, parsed[1].toolId)
        assertEquals(cards[1].status, parsed[1].status)
        assertNull(parsed[1].details)
    }
}
