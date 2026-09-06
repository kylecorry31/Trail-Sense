package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeProvider
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SatelliteFixFilterGPSModuleTest {
    private val prefs = mock<IGPSPreferences> {
        on { requiresSatelliteCount }.thenReturn(true)
    }
    private var nowMillis = 0L
    private val timeProvider = object : TimeProvider {
        override fun elapsedRealtime() = nowMillis
        override fun currentTimeMillis() = nowMillis
    }
    private val module = SatelliteFixFilterGPSModule(prefs, mock(), timeProvider)
    private val previous = ModularGPSData(time = Instant.EPOCH)

    private fun reading(satellites: Int?) = ModularGPSData(
        satellites = satellites, time = previous.time.plusSeconds(1)
    )

    @Test
    fun cachedFixesDoNotStartTheSatelliteWait() = runBlocking<Unit> {
        val older = reading(0).apply { time = previous.time.minusSeconds(1) }
        val cached = reading(0).apply { time = previous.time }
        assertTrue(module.update(previous, older))
        nowMillis = 5_000L
        assertTrue(module.update(previous, cached))
        nowMillis = 60_000L
        assertTrue(module.update(previous, cached))

        val fresh = reading(0)
        assertFalse(module.update(previous, fresh))
        nowMillis = 64_999L
        assertFalse(module.update(previous, fresh))
        nowMillis = 65_000L
        assertTrue(module.update(previous, fresh))
    }

    @Test
    fun fallbackStaysOpenUntilThePipelineAcceptsTheFirstAllowedFixOrNewer() = runBlocking<Unit> {
        previous.time = Instant.EPOCH
        val first = reading(3).apply { time = Instant.EPOCH.plusSeconds(1) }
        assertFalse(module.update(previous, first))
        nowMillis = 5_000L
        assertTrue(module.update(previous, first))
        val newer = reading(3).apply { time = Instant.EPOCH.plusSeconds(2) }
        nowMillis = 6_000L
        assertTrue(module.update(previous, newer))
        previous.time = first.time
        assertFalse(module.update(previous, newer))
        nowMillis = 11_000L
        assertTrue(module.update(previous, newer))
        previous.time = newer.time.plusSeconds(1)
        assertTrue(module.update(previous, newer))
    }

    @Test
    fun lifecycleResetsPendingFallback() = runBlocking<Unit> {
        previous.time = Instant.EPOCH
        val candidate = reading(3).apply { time = Instant.EPOCH.plusSeconds(1) }
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
    fun rejectsInsufficientSatellitesUntilTheWaitExpires() = runBlocking<Unit> {
        assertFalse(module.update(previous, reading(3)))
        nowMillis = 4_999L
        assertFalse(module.update(previous, reading(3)))
        nowMillis = 5_000L
        assertTrue(module.update(previous, reading(3)))
    }

    @Test
    fun acceptingAReadingResetsTheWait() = runBlocking<Unit> {
        assertFalse(module.update(previous, reading(3)))
        nowMillis = 4_000L
        assertTrue(module.update(previous, reading(4)))

        assertFalse(module.update(previous, reading(3)))
        nowMillis = 8_999L
        assertFalse(module.update(previous, reading(3)))
        nowMillis = 9_000L
        assertTrue(module.update(previous, reading(3)))
    }

    @Test
    fun unknownSatelliteCountAndDisabledRequirementAreAccepted() = runBlocking<Unit> {
        assertTrue(module.update(previous, reading(null)))
        assertFalse(module.update(previous, reading(3)))

        whenever(prefs.requiresSatelliteCount).thenReturn(false)
        assertTrue(module.update(previous, reading(0)))

        whenever(prefs.requiresSatelliteCount).thenReturn(true)
        assertFalse(module.update(previous, reading(3)))
    }

    @Test
    fun startingAndStoppingResetTheWait() = runBlocking<Unit> {
        assertFalse(module.update(previous, reading(3)))
        nowMillis = 4_000L
        module.stop(previous)

        nowMillis = 10_000L
        module.start(previous)
        assertFalse(module.update(previous, reading(3)))
        nowMillis = 14_999L
        assertFalse(module.update(previous, reading(3)))
        nowMillis = 15_000L
        assertTrue(module.update(previous, reading(3)))
    }
}
