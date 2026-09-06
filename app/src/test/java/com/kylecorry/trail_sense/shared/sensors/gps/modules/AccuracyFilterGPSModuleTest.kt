package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeProvider
import com.kylecorry.trail_sense.shared.sensors.gps.GPSAccuracyFilter
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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
    private val previous = ModularGPSData(time = Instant.EPOCH)
    private val moderateWait = GPSAccuracyFilter.Moderate.maxAccuracyWait!!.toMillis()
    private val highWait = GPSAccuracyFilter.High.maxAccuracyWait!!.toMillis()

    private fun reading(accuracy: Float?) = ModularGPSData(
        location = Coordinate(1.0, 1.0), hasValidReading = true, horizontalAccuracy = accuracy,
        time = previous.time.plusSeconds(1)
    )

    @Test
    fun timeoutReturnsASnapshotOfTheMostAccurateFix() = runBlocking<Unit> {
        val best = reading(20f).apply {
            location = Coordinate(2.0, 3.0)
            altitude = 123f
        }
        assertFalse(module.update(previous, best))
        // The pipeline reuses its candidate, so retaining the reference is insufficient.
        best.location = Coordinate(4.0, 5.0)
        best.horizontalAccuracy = 100f
        best.time = best.time.plusSeconds(1)
        nowMillis = moderateWait
        assertTrue(module.update(previous, best))
        assertEquals(Coordinate(2.0, 3.0), best.location)
        assertEquals(20f, best.horizontalAccuracy)
        assertEquals(123f, best.altitude)
        assertEquals(Instant.EPOCH.plusSeconds(1), best.time)
        assertEquals(Instant.EPOCH, previous.time)

        // Until a fix lands the filter stands aside instead of re-offering the selected one.
        val retry = reading(80f).apply { time = Instant.EPOCH.plusSeconds(3) }
        assertTrue(module.update(previous, retry))
        assertEquals(Instant.EPOCH.plusSeconds(3), retry.time)
        assertEquals(Coordinate(1.0, 1.0), retry.location)

        best.copyInto(previous)
        val next = reading(90f)
        assertFalse(module.update(previous, next))
        nowMillis += moderateWait
        assertTrue(module.update(previous, next))
        assertEquals(90f, next.horizontalAccuracy)
    }

    @Test
    fun theRetainedFixIsDroppedOnceItTrailsTheCurrentFix() = runBlocking<Unit> {
        val best = { reading(20f).apply { location = Coordinate(2.0, 3.0) } }
        val arrival = { seconds: Long ->
            reading(80f).apply {
                location = Coordinate(4.0, 5.0)
                time = Instant.EPOCH.plusSeconds(seconds)
            }
        }

        assertFalse(module.update(previous, best()))
        val recent = arrival(3)
        nowMillis = moderateWait
        assertTrue(module.update(previous, recent))
        assertEquals(Coordinate(2.0, 3.0), recent.location)
        assertEquals(20f, recent.horizontalAccuracy)

        module.start(previous)
        assertFalse(module.update(previous, best()))
        val stale = arrival(4)
        nowMillis += moderateWait
        assertTrue(module.update(previous, stale))
        assertEquals(Coordinate(4.0, 5.0), stale.location)
        assertEquals(80f, stale.horizontalAccuracy)
    }

    @Test
    fun awaitingAcceptanceLeavesLaterReadingsAlone() = runBlocking<Unit> {
        assertFalse(module.update(previous, reading(20f).apply { location = Coordinate(2.0, 3.0) }))

        val stale = reading(80f).apply {
            location = Coordinate(4.0, 5.0)
            time = Instant.EPOCH.plusSeconds(4)
        }
        nowMillis = moderateWait
        assertTrue(module.update(previous, stale))
        assertEquals(Coordinate(4.0, 5.0), stale.location)

        // The chosen fix reaches the pipeline once; later readings pass through as themselves.
        val retry = reading(90f).apply {
            location = Coordinate(6.0, 7.0)
            time = Instant.EPOCH.plusSeconds(5)
        }
        assertTrue(module.update(previous, retry))
        assertEquals(Coordinate(6.0, 7.0), retry.location)
        assertEquals(90f, retry.horizontalAccuracy)
    }

    @Test
    fun includesTheTimeoutArrivalAndPrefersTheLatestEquallyAccurateFix() = runBlocking<Unit> {
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = 2_000L
        assertFalse(module.update(previous, reading(30f)))
        val latest = reading(30f).apply { time = Instant.EPOCH.plusSeconds(3) }
        nowMillis = moderateWait
        assertTrue(module.update(previous, latest))
        assertEquals(30f, latest.horizontalAccuracy)
        assertEquals(Instant.EPOCH.plusSeconds(3), latest.time)
    }

    @Test
    fun resetsDiscardTheRetainedCandidate() = runBlocking<Unit> {
        val resets: List<suspend () -> Unit> = listOf(
            { module.stop(previous); module.start(previous) },
            { module.update(previous, reading(5f)) },
            { module.update(previous, reading(null)) },
            {
                whenever(prefs.accuracyFilter).thenReturn(GPSAccuracyFilter.None)
                module.update(previous, reading(100f))
                whenever(prefs.accuracyFilter).thenReturn(GPSAccuracyFilter.Moderate)
            }
        )
        for (reset in resets) {
            module.start(previous)
            assertFalse(module.update(previous, reading(20f)))
            reset()
            assertFalse(module.update(previous, reading(80f)))
            nowMillis += moderateWait
            val fallback = reading(100f)
            assertTrue(module.update(previous, fallback))
            assertEquals(80f, fallback.horizontalAccuracy)
        }
    }

    @Test
    fun cachedFixesDoNotStartTheAccuracyWait() = runBlocking<Unit> {
        previous.time = Instant.EPOCH.plusSeconds(10)
        val older = reading(100f).apply { time = previous.time.minusSeconds(1) }
        val cached = reading(100f).apply { time = previous.time }
        assertTrue(module.update(previous, older))
        nowMillis = 5_000L
        assertTrue(module.update(previous, cached))
        nowMillis = 60_000L
        assertTrue(module.update(previous, cached))

        val fresh = reading(100f)
        assertFalse(module.update(previous, fresh))
        nowMillis = 60_000L + moderateWait - 1
        assertFalse(module.update(previous, fresh))
        nowMillis = 60_000L + moderateWait
        assertTrue(module.update(previous, fresh))
    }

    @Test
    fun fallbackStaysOpenUntilThePipelineAcceptsTheFirstAllowedFixOrNewer() = runBlocking<Unit> {
        previous.time = Instant.EPOCH
        val first = reading(100f).apply { time = Instant.EPOCH.plusSeconds(1) }
        assertFalse(module.update(previous, first))
        nowMillis = moderateWait
        assertTrue(module.update(previous, first))
        val newer = reading(100f).apply { time = Instant.EPOCH.plusSeconds(2) }
        nowMillis += 1_000L
        assertTrue(module.update(previous, newer))
        assertEquals(Instant.EPOCH.plusSeconds(2), newer.time)
        previous.time = first.time
        newer.time = first.time.plusSeconds(1)
        assertFalse(module.update(previous, newer))
        nowMillis += moderateWait
        assertTrue(module.update(previous, newer))
        previous.time = newer.time.plusSeconds(1)
        assertTrue(module.update(previous, newer))
    }

    @Test
    fun lifecycleResetsPendingFallback() = runBlocking<Unit> {
        previous.time = Instant.EPOCH
        val candidate = reading(100f).apply { time = Instant.EPOCH.plusSeconds(1) }
        assertFalse(module.update(previous, candidate))
        nowMillis = moderateWait
        assertTrue(module.update(previous, candidate))
        module.stop(previous)
        assertFalse(module.update(previous, candidate))
        nowMillis += moderateWait
        assertTrue(module.update(previous, candidate))
        module.start(previous)
        assertFalse(module.update(previous, candidate))
    }

    @Test
    fun acceptsEveryReadingWhenNoAccuracyIsRequired() = runBlocking<Unit> {
        whenever(prefs.accuracyFilter).thenReturn(GPSAccuracyFilter.None)
        repeat(10) {
            assertTrue(module.update(previous, reading(1000f)))
        }
    }

    @Test
    fun acceptsReadingsMeetingTheRequirement() = runBlocking<Unit> {
        assertTrue(module.update(previous, reading(16f)))
        assertTrue(module.update(previous, reading(1f)))
    }

    @Test
    fun acceptsReadingsWithAnUnusableAccuracy() = runBlocking<Unit> {
        assertTrue(module.update(previous, reading(null)))
        assertTrue(module.update(previous, reading(0f)))
        assertTrue(module.update(previous, reading(-1f)))
    }

    @Test
    fun mediumAcceptsAfterItsWaitRegardlessOfCallbackCount() = runBlocking<Unit> {
        val candidate = reading(17f)
        assertFalse(module.update(previous, candidate))
        nowMillis = moderateWait - 1
        repeat(100) {
            assertFalse(module.update(previous, candidate))
        }
        nowMillis = moderateWait
        assertTrue(module.update(previous, candidate))

        // Only pipeline acceptance starts a fresh wait.
        candidate.copyInto(previous)
        assertFalse(module.update(previous, reading(17f)))
        nowMillis = 2 * moderateWait - 1
        assertFalse(module.update(previous, reading(17f)))
        nowMillis = 2 * moderateWait
        assertTrue(module.update(previous, reading(17f)))
    }

    @Test
    fun accurateReadingResetsTheWait() = runBlocking<Unit> {
        val resetAt = moderateWait / 2
        assertFalse(module.update(previous, reading(17f)))
        nowMillis = resetAt
        assertTrue(module.update(previous, reading(5f)))
        assertFalse(module.update(previous, reading(17f)))
        // The original deadline passes without an acceptance because the wait restarted.
        nowMillis = moderateWait
        assertFalse(module.update(previous, reading(17f)))
        nowMillis = resetAt + moderateWait
        assertTrue(module.update(previous, reading(17f)))
    }

    @Test
    fun highAcceptsAfterItsWaitEvenWithSparseCallbacks() = runBlocking<Unit> {
        whenever(prefs.accuracyFilter).thenReturn(GPSAccuracyFilter.High)
        assertTrue(module.update(previous, reading(8f)))
        assertFalse(module.update(previous, reading(9f)))
        nowMillis = highWait - 1
        assertFalse(module.update(previous, reading(9f)))
        nowMillis = highWait
        assertTrue(module.update(previous, reading(9f)))
    }

    @Test
    fun restartingResetsTheWait() = runBlocking<Unit> {
        module.start(previous)
        assertFalse(module.update(previous, reading(100f)))
        module.stop(previous)
        nowMillis = 60_000L
        module.start(previous)
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = 60_000L + moderateWait
        assertTrue(module.update(previous, reading(100f)))
    }

    @Test
    fun unknownAccuracyResetsTheWait() = runBlocking<Unit> {
        val resetAt = moderateWait / 2
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = resetAt
        assertTrue(module.update(previous, reading(null)))
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = moderateWait
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = resetAt + moderateWait
        assertTrue(module.update(previous, reading(100f)))
    }

    @Test
    fun disablingAccuracyFilterResetsTheWait() = runBlocking<Unit> {
        val resetAt = moderateWait / 2
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = resetAt
        whenever(prefs.accuracyFilter).thenReturn(GPSAccuracyFilter.None)
        assertTrue(module.update(previous, reading(100f)))
        whenever(prefs.accuracyFilter).thenReturn(GPSAccuracyFilter.Moderate)
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = moderateWait
        assertFalse(module.update(previous, reading(100f)))
        nowMillis = resetAt + moderateWait
        assertTrue(module.update(previous, reading(100f)))
    }

    @Test
    fun doesNotModifyEitherReading() = runBlocking<Unit> {
        val candidate = reading(17f)
        assertFalse(module.update(previous, candidate))
        assertTrue(previous.horizontalAccuracy == null)
        assertTrue(candidate.horizontalAccuracy == 17f)
        assertTrue(previous.location == Coordinate.zero)
    }
}
