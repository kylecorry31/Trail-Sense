package com.kylecorry.trail_sense.shared.andromeda_temp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

internal class ThresholdChangeTriggerTest {

    @Test
    fun updateDoesNotTriggerWithinThreshold() {
        val trigger = ThresholdChangeTrigger(0f, 1f) { previous, current ->
            abs(current - previous)
        }

        assertFalse(trigger.update(0.5f))
        assertEquals(0f, trigger.lastTriggeredValue)
    }

    @Test
    fun updateTriggersBeyondThreshold() {
        val trigger = ThresholdChangeTrigger(0f, 1f) { previous, current ->
            abs(current - previous)
        }

        assertTrue(trigger.update(1.1f))
        assertEquals(1.1f, trigger.lastTriggeredValue)
    }

    @Test
    fun updateComparesAgainstLastTriggeredValue() {
        val trigger = ThresholdChangeTrigger(0f, 1f) { previous, current ->
            abs(current - previous)
        }

        assertFalse(trigger.update(0.6f))
        assertFalse(trigger.update(0.9f))
        assertTrue(trigger.update(1.1f))
        assertEquals(1.1f, trigger.lastTriggeredValue)
    }

    @Test
    fun resetChangesLastTriggeredValue() {
        val trigger = ThresholdChangeTrigger(0f, 1f) { previous, current ->
            abs(current - previous)
        }

        trigger.reset(10f)

        assertFalse(trigger.update(10.5f))
        assertEquals(10f, trigger.lastTriggeredValue)
    }
}
