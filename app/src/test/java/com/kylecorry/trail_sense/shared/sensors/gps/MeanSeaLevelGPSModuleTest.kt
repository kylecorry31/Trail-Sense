package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.GeoidService
import com.kylecorry.trail_sense.shared.UserPreferences
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MeanSeaLevelGPSModuleTest {
    private val prefs = mock<UserPreferences>()
    private val lookups = mutableListOf<Coordinate>()
    private val module = MeanSeaLevelGPSModule(
        prefs,
        geoidService = object : GeoidService {
            override suspend fun getGeoid(location: Coordinate): Float {
                lookups.add(location)
                return 25f
            }

            override fun isSameGeoid(location1: Coordinate, location2: Coordinate): Boolean {
                return true
            }
        }
    )
    private val previous = ModularGPSData(altitude = 50f)

    private fun reading(msl: Float? = null) = ModularGPSData(
        location = Coordinate(42.0, -72.0), altitude = 100f, mslAltitude = msl
    )

    @Test
    fun correctsAltitudeUsingGeoidWithoutChangingPreviousReading() {
        val candidate = reading()
        assertTrue(module.update(previous, candidate))
        assertEquals(75f, candidate.altitude)
        assertEquals(50f, previous.altitude)
        assertEquals(listOf(candidate.location), lookups)
    }

    @Test
    fun reusesGeoidWithinSameCell() {
        module.update(previous, reading())
        val candidate = reading().apply { location = Coordinate(42.00001, -72.0) }
        module.update(previous, candidate)
        assertEquals(75f, candidate.altitude)
        assertEquals(1, lookups.size)
    }

    @Test
    fun prefersNmeaAltitudeWhenEnabled() {
        whenever(prefs.useNMEA).thenReturn(true)
        val candidate = reading(80f)
        module.update(previous, candidate)
        assertEquals(80f, candidate.altitude)
        assertEquals(80f, candidate.mslAltitude)
        assertTrue(lookups.isEmpty())
    }

    @Test
    fun retainsLastNmeaOffsetWhenLaterReadingOmitsIt() {
        whenever(prefs.useNMEA).thenReturn(true)
        module.update(previous, reading(80f))
        val candidate = reading().apply { altitude = 120f }
        module.update(previous, candidate)
        assertEquals(100f, candidate.altitude)
        assertTrue(lookups.isEmpty())
    }

    @Test
    fun fallsBackToGeoidWhenNmeaOffsetIsZero() {
        whenever(prefs.useNMEA).thenReturn(true)
        val candidate = reading(100f)
        module.update(previous, candidate)
        assertEquals(75f, candidate.altitude)
        assertEquals(1, lookups.size)
    }

    @Test
    fun ignoresNmeaOffsetWhenDisabled() {
        val candidate = reading(80f)
        module.update(previous, candidate)
        assertEquals(75f, candidate.altitude)
        assertEquals(1, lookups.size)
    }
}
