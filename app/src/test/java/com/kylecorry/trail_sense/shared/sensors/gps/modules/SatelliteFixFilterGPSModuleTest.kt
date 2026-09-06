package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeProvider
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SatelliteFixFilterGPSModuleTest {
    private val prefs = mock<IGPSPreferences> {
        on { requiresSatellites }.thenReturn(true)
    }
    private var nowMillis = 0L
    private val timeProvider = object : TimeProvider {
        override fun elapsedRealtime() = nowMillis
        override fun currentTimeMillis() = nowMillis
    }
    private val module = SatelliteFixFilterGPSModule(prefs, mock(), timeProvider)
    private val previous = ModularGPSData()

    private fun reading(satellites: Int?) = ModularGPSData(satellites = satellites)

    @Test
    fun rejectsInsufficientSatellitesUntilTheWaitExpires() {
        assertFalse(module.update(previous, reading(3)))
        nowMillis = 4_999L
        assertFalse(module.update(previous, reading(3)))
        nowMillis = 5_000L
        assertTrue(module.update(previous, reading(3)))
    }

    @Test
    fun acceptingAReadingResetsTheWait() {
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
    fun unknownSatelliteCountAndDisabledRequirementAreAccepted() {
        assertTrue(module.update(previous, reading(null)))
        assertFalse(module.update(previous, reading(3)))

        whenever(prefs.requiresSatellites).thenReturn(false)
        assertTrue(module.update(previous, reading(0)))

        whenever(prefs.requiresSatellites).thenReturn(true)
        assertFalse(module.update(previous, reading(3)))
    }

    @Test
    fun startingAndStoppingResetTheWait() {
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
