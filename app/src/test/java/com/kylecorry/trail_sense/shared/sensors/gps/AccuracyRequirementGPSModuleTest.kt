package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AccuracyRequirementGPSModuleTest {
    private val prefs = mock<IGPSPreferences> {
        on { accuracyRequirement }.thenReturn(GPSAccuracyRequirement.Medium)
    }
    private val module = AccuracyRequirementGPSModule(prefs, mock())
    private val previous = ModularGPSData()

    private fun reading(accuracy: Float?) = ModularGPSData(
        location = Coordinate(1.0, 1.0), hasValidReading = true, horizontalAccuracy = accuracy
    )

    @Test
    fun acceptsEveryReadingWhenNoAccuracyIsRequired() {
        whenever(prefs.accuracyRequirement).thenReturn(GPSAccuracyRequirement.Low)
        repeat(10) {
            assertTrue(module.update(previous, reading(1000f)))
        }
    }

    @Test
    fun acceptsReadingsMeetingTheRequirement() {
        assertTrue(module.update(previous, reading(16f)))
        assertTrue(module.update(previous, reading(1f)))
    }

    @Test
    fun acceptsReadingsWithAnUnusableAccuracy() {
        assertTrue(module.update(previous, reading(null)))
        assertTrue(module.update(previous, reading(0f)))
        assertTrue(module.update(previous, reading(-1f)))
    }

    @Test
    fun acceptsAnInaccurateReadingOnceTheRejectionLimitIsHit() {
        repeat(3) {
            assertFalse(module.update(previous, reading(17f)))
        }
        assertTrue(module.update(previous, reading(17f)))
    }

    @Test
    fun countsRejectionsOnlyWhenConsecutive() {
        assertFalse(module.update(previous, reading(17f)))
        assertFalse(module.update(previous, reading(17f)))
        assertTrue(module.update(previous, reading(5f)))
        repeat(3) {
            assertFalse(module.update(previous, reading(17f)))
        }
        assertTrue(module.update(previous, reading(17f)))
    }

    @Test
    fun usesTheHighRequirementThresholdAndLimit() {
        whenever(prefs.accuracyRequirement).thenReturn(GPSAccuracyRequirement.High)
        assertTrue(module.update(previous, reading(8f)))
        repeat(4) {
            assertFalse(module.update(previous, reading(9f)))
        }
        assertTrue(module.update(previous, reading(9f)))
    }

    @Test
    fun bypassesAccuracyWhenTimedOutAndResetsRejections() {
        repeat(2) {
            assertFalse(module.update(previous, reading(100f)))
        }
        previous.isTimedOut = true
        assertTrue(module.update(previous, reading(100f)))
        previous.isTimedOut = false
        repeat(3) {
            assertFalse(module.update(previous, reading(100f)))
        }
    }

    @Test
    fun doesNotModifyEitherReading() {
        val candidate = reading(17f)
        assertFalse(module.update(previous, candidate))
        assertTrue(previous.horizontalAccuracy == null)
        assertTrue(candidate.horizontalAccuracy == 17f)
        assertTrue(previous.location == Coordinate.zero)
    }
}
