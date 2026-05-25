package com.kylecorry.trail_sense.tools.ai_assistant.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AiToolKnowledgeParserTest {

    @Test
    fun `parse reads complete knowledge blocks`() {
        val entries = AiToolKnowledgeParser.parse(
            """
                # Knowledge
                
                ## Weather
                Tool ID: 20
                Needs: pressure, forecast
                Where: Open Weather
                How: Enable the monitor
                Values: Falling pressure can indicate storms
                Caveats: Requires a barometer
                Related: Clouds
            """.trimIndent()
        )

        assertEquals(1, entries.size)
        assertEquals(20L, entries[0].toolId)
        assertEquals("pressure, forecast", entries[0].needs)
        assertEquals("Open Weather", entries[0].where)
        assertEquals("Enable the monitor", entries[0].how)
        assertEquals("Falling pressure can indicate storms", entries[0].values)
        assertEquals("Requires a barometer", entries[0].caveats)
        assertEquals("Clouds", entries[0].related)
    }

    @Test
    fun `parse ignores malformed blocks`() {
        val entries = AiToolKnowledgeParser.parse(
            """
                ## Broken
                Tool ID: nope
                Needs: pressure
                
                ## Complete
                Tool ID: 6
                Needs: compass
                Where: Open Navigation
                How: Use the compass
                Values: Bearing is direction in degrees
                Caveats: Calibrate the compass
            """.trimIndent()
        )

        assertEquals(1, entries.size)
        assertEquals(6L, entries[0].toolId)
    }
}
