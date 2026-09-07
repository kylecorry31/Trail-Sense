package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.settings.migrations.InMemoryPreferences
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.SpeedSource
import com.kylecorry.trail_sense.shared.sensors.gps.GPSKalmanState
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CacheGPSModuleTest {
    private val preferences = InMemoryPreferences()
    private val module = CacheGPSModule(preferences)
    private val previous = ModularGPSData()

    private fun reading() = ModularGPSData(
        location = Coordinate(42.0, -72.0), altitude = 123f,
        time = Instant.parse("2020-01-01T00:00:00Z"),
        speed = Speed.from(3f, DistanceUnits.Meters, TimeUnits.Seconds),
        horizontalAccuracy = 5f, verticalAccuracy = 8f
    )

    @Test
    fun preservesSpeedSourceAndTreatsLegacyCacheAsUnknown() = runBlocking<Unit> {
        val restored = ModularGPSData()
        for (source in SpeedSource.entries) {
            module.update(previous, reading().apply { speedSource = source })
            module.restore(restored)
            assertEquals(source, restored.speedSource)
        }
        preferences.putString(CacheGPSModule.LAST_GPS, """{"speed":3,"bearing":90}""")
        module.restore(restored)
        assertEquals(SpeedSource.Unknown, restored.speedSource)
        assertEquals(3f, restored.speed.value)
    }

    @Test
    fun persistsFilterStateSeparatelyFromReportedAccuracy() = runBlocking<Unit> {
        val candidate = reading().apply {
            kalmanState = GPSKalmanState(
                listOf(1f, 2f, 3f, 4f),
                List(4) { row -> List(4) { column -> if (row == column) 2f else 0f } },
                42.1,
                -72.1
            )
        }
        module.update(previous, candidate)
        val restored = ModularGPSData()
        CacheGPSModule(preferences).restore(restored)
        assertEquals(candidate.kalmanState, restored.kalmanState)
        assertEquals(candidate.horizontalAccuracy, restored.horizontalAccuracy)

        // A reading accepted with smoothing disabled must clear old filter state.
        module.update(restored, reading())
        module.restore(restored)
        assertNull(restored.kalmanState)
    }

    @Test
    fun cachesBearingAndRemovesItWhenUnavailable() = runBlocking<Unit> {
        val candidate = reading().apply { rawBearing = 123f }
        module.update(previous, candidate)
        val restored = ModularGPSData()
        CacheGPSModule(preferences).restore(restored)
        assertEquals(123f, restored.rawBearing)
        assertEquals(Bearing.from(123f), restored.bearing)

        module.update(previous, reading())
        module.restore(restored)
        assertNull(restored.rawBearing)
        assertNull(restored.bearing)
    }

    @Test
    fun cachesBearingWhenRawBearingIsUnavailable() = runBlocking<Unit> {
        module.update(previous, reading().apply { bearing = Bearing.from(90f) })
        val restored = ModularGPSData()
        module.restore(restored)
        assertEquals(90f, restored.rawBearing)
        assertEquals(Bearing.from(90f), restored.bearing)
    }

    @Test
    fun restoresPersistedFieldsAcrossModuleInstances() = runBlocking<Unit> {
        val candidate = reading()
        assertTrue(module.update(previous, candidate))
        val restored = ModularGPSData()
        CacheGPSModule(preferences).restore(restored)
        assertEquals(candidate.location, restored.location)
        assertEquals(candidate.altitude, restored.altitude)
        assertEquals(candidate.time, restored.time)
        assertEquals(candidate.speed, restored.speed)
        assertEquals(candidate.horizontalAccuracy, restored.horizontalAccuracy)
        assertEquals(candidate.verticalAccuracy, restored.verticalAccuracy)
        assertEquals(Coordinate.zero, previous.location)
    }

    @Test
    fun persistsTheCacheAsOneJsonPreference() = runBlocking<Unit> {
        val candidate = reading().apply {
            rawBearing = 123f
            kalmanState = GPSKalmanState(
                listOf(1f, 2f, 3f, 4f),
                List(4) { List(4) { 0f } },
                42.0,
                -72.0
            )
        }

        module.update(previous, candidate)

        assertTrue(preferences.contains(CacheGPSModule.LAST_GPS))
    }

    @Test
    fun missingAccuraciesRemovePreviouslyCachedValues() = runBlocking<Unit> {
        module.update(previous, reading())
        module.update(previous, reading().apply {
            horizontalAccuracy = null
            verticalAccuracy = null
        })
        val restored = reading()
        module.restore(restored)
        assertNull(restored.horizontalAccuracy)
        assertNull(restored.verticalAccuracy)
    }

    @Test
    fun restoreClearsFieldsThatAreNotPersisted() = runBlocking<Unit> {
        module.update(previous, reading())
        val restored = ModularGPSData(
            satellites = 8, satelliteDetails = emptyList(), mslAltitude = 10f,
            rawBearing = 20f, bearing = Bearing.from(20f), bearingAccuracy = 1f,
            speedAccuracy = 2f, fixTimeElapsedNanos = 123L
        )
        module.restore(restored)
        assertEquals(Quality.Unknown, restored.quality)
        assertNull(restored.satellites)
        assertNull(restored.satelliteDetails)
        assertNull(restored.mslAltitude)
        assertNull(restored.rawBearing)
        assertNull(restored.bearing)
        assertNull(restored.bearingAccuracy)
        assertNull(restored.speedAccuracy)
        assertNull(restored.fixTimeElapsedNanos)
    }

    @Test
    fun emptyCacheRestoresDefaults() = runBlocking<Unit> {
        val restored = reading()
        module.restore(restored)
        assertEquals(Coordinate.zero, restored.location)
        assertEquals(Instant.EPOCH, restored.time)
        assertEquals(0f, restored.altitude)
        assertEquals(0f, restored.speed.value)
        assertNull(restored.horizontalAccuracy)
        assertNull(restored.verticalAccuracy)
    }

    @Test
    fun onlyPastReadingsNewerThanCurrentDataAreRestorable() = runBlocking<Unit> {
        val candidate = reading()
        module.update(previous, candidate)
        assertTrue(module.hasNewerReading(ModularGPSData(time = Instant.EPOCH)))
        assertFalse(module.hasNewerReading(candidate))
        assertFalse(module.hasNewerReading(ModularGPSData(time = candidate.time.plusSeconds(1))))
        module.update(previous, candidate.apply { time = Instant.now().plusSeconds(3600) })
        assertFalse(module.hasNewerReading(ModularGPSData(time = Instant.EPOCH)))
    }
}
