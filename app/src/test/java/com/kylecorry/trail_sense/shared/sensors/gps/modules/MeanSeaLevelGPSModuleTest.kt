package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.GeoidService
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MeanSeaLevelGPSModuleTest {
    private val prefs = mock<IGPSPreferences>()
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
    private val previous = ModularGPSData(altitude = 50f, time = Instant.EPOCH)

    private fun reading(msl: Float? = null) = ModularGPSData(
        location = Coordinate(42.0, -72.0),
        altitude = 100f,
        mslAltitude = msl,
        time = Instant.EPOCH.plusSeconds(1)
    )

    @Test
    fun awaitsGeoidForNewCellBeforeApplyingAltitude() = runBlocking<Unit> {
        val requested = kotlinx.coroutines.CompletableDeferred<Unit>()
        val offset = kotlinx.coroutines.CompletableDeferred<Float>()
        val module = MeanSeaLevelGPSModule(prefs, object : GeoidService {
            override suspend fun getGeoid(location: Coordinate): Float {
                if (location.longitude == -72.0) return 25f
                requested.complete(Unit)
                return offset.await()
            }

            override fun isSameGeoid(location1: Coordinate, location2: Coordinate) =
                location1 == location2
        })
        module.update(previous, reading())
        val candidate = reading().apply { location = Coordinate(42.0, -73.0) }
        val update = async {
            module.update(previous, candidate)
        }
        requested.await()
        assertFalse(update.isCompleted)
        assertEquals(100f, candidate.altitude)
        offset.complete(40f)
        assertTrue(update.await())
        assertEquals(60f, candidate.altitude)
    }

    @Test
    fun correctsAltitudeUsingGeoidWithoutChangingPreviousReading() = runBlocking<Unit> {
        val candidate = reading()
        assertTrue(module.update(previous, candidate))
        assertEquals(75f, candidate.altitude)
        assertEquals(50f, previous.altitude)
        assertEquals(listOf(candidate.location), lookups)
    }

    @Test
    fun reusesGeoidWithinSameCell() = runBlocking<Unit> {
        module.update(previous, reading())
        val candidate = reading().apply { location = Coordinate(42.00001, -72.0) }
        module.update(previous, candidate)
        assertEquals(75f, candidate.altitude)
        assertEquals(1, lookups.size)
    }

    @Test
    fun prefersNmeaAltitudeWhenEnabled() = runBlocking<Unit> {
        whenever(prefs.useNMEA).thenReturn(true)
        val candidate = reading(80f)
        module.update(previous, candidate)
        assertEquals(80f, candidate.altitude)
        assertEquals(80f, candidate.mslAltitude)
        assertTrue(lookups.isEmpty())
    }

    @Test
    fun retainsLastNmeaOffsetWhenLaterReadingOmitsIt() = runBlocking<Unit> {
        whenever(prefs.useNMEA).thenReturn(true)
        module.update(previous, reading(80f))
        val candidate = reading().apply { altitude = 120f }
        module.update(previous, candidate)
        assertEquals(100f, candidate.altitude)
        assertTrue(lookups.isEmpty())
    }

    @Test
    fun fallsBackToGeoidWhenNmeaOffsetIsZero() = runBlocking<Unit> {
        whenever(prefs.useNMEA).thenReturn(true)
        val candidate = reading(100f)
        module.update(previous, candidate)
        assertEquals(75f, candidate.altitude)
        assertEquals(1, lookups.size)
    }

    @Test
    fun doesNotCorrectTheAltitudeOfARepeatedFix() = runBlocking<Unit> {
        val candidate = reading().apply { time = previous.time }
        assertTrue(module.update(previous, candidate))
        assertEquals(100f, candidate.altitude)
        assertTrue(lookups.isEmpty())
    }

    @Test
    fun ignoresNmeaOffsetWhenDisabled() = runBlocking<Unit> {
        val candidate = reading(80f)
        module.update(previous, candidate)
        assertEquals(75f, candidate.altitude)
        assertEquals(1, lookups.size)
    }
}
