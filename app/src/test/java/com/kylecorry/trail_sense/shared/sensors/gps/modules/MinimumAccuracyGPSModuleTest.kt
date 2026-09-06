package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.sensors.gps.GPSAccuracyRequirement
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MinimumAccuracyGPSModuleTest {
    private val prefs = mock<IGPSPreferences> {
        on { accuracyRequirement }.thenReturn(GPSAccuracyRequirement.Medium)
    }
    private var nowMillis = 0L
    private val module = MinimumAccuracyGPSModule(prefs, mock()) { nowMillis }
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
    fun mediumAcceptsAfterFiveSecondsRegardlessOfCallbackCount() {
        val candidate = reading(17f)
        assertFalse(module.update(previous, candidate))
        nowMillis = 4_999L
        repeat(100) {
            assertFalse(module.update(previous, candidate))
        }
        nowMillis = 5_000L
        assertTrue(module.update(previous, candidate))

        // Accepting a fallback starts a fresh wait for the next inaccurate reading.
        assertFalse(module.update(previous, reading(17f)))
        nowMillis = 9_999L
        assertFalse(module.update(previous, reading(17f)))
        nowMillis = 10_000L
        assertTrue(module.update(previous, reading(17f)))
    }

    @Test
    fun accurateReadingResetsTheWait() {
        assertFalse(module.update(previous, reading(17f)))
        nowMillis = 4_000L
        assertTrue(module.update(previous, reading(5f)))
        assertFalse(module.update(previous, reading(17f)))
        nowMillis = 5_000L
        assertFalse(module.update(previous, reading(17f)))
        nowMillis = 9_000L
        assertTrue(module.update(previous, reading(17f)))
    }

    @Test
    fun highAcceptsAfterTenSecondsEvenWithSparseCallbacks() {
        whenever(prefs.accuracyRequirement).thenReturn(GPSAccuracyRequirement.High)
        assertTrue(module.update(previous, reading(8f)))
        assertFalse(module.update(previous, reading(9f)))
        nowMillis = 9_999L
        assertFalse(module.update(previous, reading(9f)))
        nowMillis = 10_000L
        assertTrue(module.update(previous, reading(9f)))
    }

    @Test
    fun restartingResetsTheWait() {
        module.start(previous)
        assertFalse(module.update(previous, reading(100f)))
        module.stop(previous)
        nowMillis = 60_000L
        module.start(previous)
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = 65_000L
        assertTrue(module.update(previous, reading(100f)))
    }

    @Test
    fun unknownAccuracyResetsTheWait() {
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = 4_000L
        assertTrue(module.update(previous, reading(null)))
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = 5_000L
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = 9_000L
        assertTrue(module.update(previous, reading(100f)))
    }

    @Test
    fun disablingAccuracyRequirementResetsTheWait() {
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = 4_000L
        whenever(prefs.accuracyRequirement).thenReturn(GPSAccuracyRequirement.Low)
        assertTrue(module.update(previous, reading(100f)))
        whenever(prefs.accuracyRequirement).thenReturn(GPSAccuracyRequirement.Medium)
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = 5_000L
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = 9_000L
        assertTrue(module.update(previous, reading(100f)))
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
