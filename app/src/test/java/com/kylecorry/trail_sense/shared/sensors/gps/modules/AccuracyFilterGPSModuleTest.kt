package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeProvider
import com.kylecorry.trail_sense.shared.sensors.gps.GPSAccuracyFilter
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

class AccuracyFilterGPSModuleTest {
    private val prefs = mock<IGPSPreferences> {
        on { accuracyFilter }.thenReturn(GPSAccuracyFilter.Moderate)
    }
    private var nowMillis = 0L
    private val timeProvider = object : TimeProvider {
        override fun elapsedRealtime() = nowMillis
        override fun currentTimeMillis() = nowMillis
    }
    private val module = AccuracyFilterGPSModule(prefs, mock(), timeProvider)
    private val previous = ModularGPSData()

    private fun reading(accuracy: Float?) = ModularGPSData(
        location = Coordinate(1.0, 1.0), hasValidReading = true, horizontalAccuracy = accuracy
    )

    @Test
    fun fallbackStaysOpenUntilThePipelineAcceptsTheFirstAllowedFixOrNewer() {
        previous.time = Instant.EPOCH
        val first = reading(100f).apply { time = Instant.EPOCH.plusSeconds(1) }
        assertFalse(module.update(previous, first))
        nowMillis = 5_000L
        assertTrue(module.update(previous, first))
        val newer = reading(100f).apply { time = Instant.EPOCH.plusSeconds(2) }
        nowMillis = 6_000L
        assertTrue(module.update(previous, newer))
        previous.time = first.time
        assertFalse(module.update(previous, newer))
        nowMillis = 11_000L
        assertTrue(module.update(previous, newer))
        previous.time = newer.time.plusSeconds(1)
        assertFalse(module.update(previous, newer))
    }

    @Test
    fun lifecycleResetsPendingFallback() {
        previous.time = Instant.EPOCH
        val candidate = reading(100f).apply { time = Instant.EPOCH.plusSeconds(1) }
        assertFalse(module.update(previous, candidate))
        nowMillis = 5_000L
        assertTrue(module.update(previous, candidate))
        module.stop(previous)
        assertFalse(module.update(previous, candidate))
        nowMillis = 10_000L
        assertTrue(module.update(previous, candidate))
        module.start(previous)
        assertFalse(module.update(previous, candidate))
    }

    @Test
    fun acceptsEveryReadingWhenNoAccuracyIsRequired() {
        whenever(prefs.accuracyFilter).thenReturn(GPSAccuracyFilter.None)
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

        // Only pipeline acceptance starts a fresh wait.
        candidate.copyInto(previous)
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
        whenever(prefs.accuracyFilter).thenReturn(GPSAccuracyFilter.High)
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
    fun disablingAccuracyFilterResetsTheWait() {
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = 4_000L
        whenever(prefs.accuracyFilter).thenReturn(GPSAccuracyFilter.None)
        assertTrue(module.update(previous, reading(100f)))
        whenever(prefs.accuracyFilter).thenReturn(GPSAccuracyFilter.Moderate)
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
