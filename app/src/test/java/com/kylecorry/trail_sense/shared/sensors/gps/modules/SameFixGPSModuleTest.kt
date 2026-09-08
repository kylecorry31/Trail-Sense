package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.sensors.gps.GPSKalmanState
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.SpeedSource
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SameFixGPSModuleTest {
    private val module = SameFixGPSModule()

    private val kalmanState = GPSKalmanState(
        state = listOf(1f, 2f, 3f, 4f),
        covariance = List(4) { List(4) { 0f } },
        referenceLatitude = 1.0,
        referenceLongitude = 2.0
    )

    private val previous = ModularGPSData(
        satellites = 4,
        location = Coordinate(1.0, 2.0),
        horizontalAccuracy = 3f,
        verticalAccuracy = 4f,
        mslAltitude = 5f,
        bearing = Bearing.from(10f),
        rawBearing = 10f,
        bearingAccuracy = 6f,
        speedAccuracy = 7f,
        fixTimeElapsedNanos = 8L,
        quality = Quality.Good,
        hasValidReading = true,
        altitude = 9f,
        time = Instant.EPOCH.plusSeconds(1),
        speed = Speed.from(10f, DistanceUnits.Meters, TimeUnits.Seconds),
        isTimedOut = true
    ).apply {
        kalmanState = this@SameFixGPSModuleTest.kalmanState
        speedSource = SpeedSource.PositionDerived
    }

    private fun reading(seconds: Long) = ModularGPSData(
        satellites = 11,
        location = Coordinate(3.0, 4.0),
        horizontalAccuracy = 12f,
        verticalAccuracy = 13f,
        mslAltitude = 14f,
        bearing = Bearing.from(20f),
        rawBearing = 20f,
        bearingAccuracy = 15f,
        speedAccuracy = 16f,
        fixTimeElapsedNanos = 17L,
        quality = Quality.Poor,
        hasValidReading = true,
        altitude = 18f,
        time = Instant.EPOCH.plusSeconds(seconds),
        speed = Speed.from(19f, DistanceUnits.Meters, TimeUnits.Seconds)
    ).apply { speedSource = SpeedSource.Provider }

    @Test
    fun restoresFixFieldsWhenTheFixRepeats() = runBlocking<Unit> {
        val candidate = reading(1).apply {
            satelliteDetails = emptyList()
        }
        assertTrue(module.update(previous, candidate))

        // The satellite and NMEA fields are kept
        assertEquals(11, candidate.satellites)
        assertEquals(emptyList<Any>(), candidate.satelliteDetails)
        assertEquals(14f, candidate.mslAltitude)

        assertEquals(previous.location, candidate.location)
        assertEquals(previous.altitude, candidate.altitude)
        assertEquals(previous.speed, candidate.speed)
        assertEquals(previous.speedSource, candidate.speedSource)
        assertEquals(previous.horizontalAccuracy, candidate.horizontalAccuracy)
        assertEquals(previous.verticalAccuracy, candidate.verticalAccuracy)
        assertEquals(previous.bearing, candidate.bearing)
        assertEquals(previous.rawBearing, candidate.rawBearing)
        assertEquals(previous.bearingAccuracy, candidate.bearingAccuracy)
        assertEquals(previous.speedAccuracy, candidate.speedAccuracy)
        assertEquals(previous.fixTimeElapsedNanos, candidate.fixTimeElapsedNanos)
        assertEquals(previous.quality, candidate.quality)
        assertEquals(previous.time, candidate.time)
        assertEquals(kalmanState, candidate.kalmanState)
        assertTrue(candidate.isTimedOut)
    }

    @Test
    fun leavesNewFixesAlone() = runBlocking<Unit> {
        val candidate = reading(2)
        assertTrue(module.update(previous, candidate))
        assertEquals(Coordinate(3.0, 4.0), candidate.location)
        assertEquals(18f, candidate.altitude)
        assertNull(candidate.kalmanState)
    }

    @Test
    fun leavesTheFirstFixAlone() = runBlocking<Unit> {
        val previous = ModularGPSData(time = Instant.EPOCH.plusSeconds(1))
        val candidate = reading(1)
        assertTrue(module.update(previous, candidate))
        assertEquals(Coordinate(3.0, 4.0), candidate.location)
        assertEquals(18f, candidate.altitude)
    }
}
