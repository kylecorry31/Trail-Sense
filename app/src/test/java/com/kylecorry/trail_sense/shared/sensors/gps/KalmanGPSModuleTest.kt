package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.Instant

class KalmanGPSModuleTest {
    private val module = KalmanGPSModule(mock())
    private val previous = ModularGPSData(time = Instant.EPOCH)

    private fun reading(seconds: Long, longitude: Double = 1.0) = ModularGPSData(
        location = Coordinate(1.0, longitude),
        time = Instant.EPOCH.plusSeconds(seconds),
        horizontalAccuracy = 10f,
        fixTimeElapsedNanos = seconds * 1_000_000_000
    )

    @Test
    fun predictsUsingCachedVelocityInEachDirection() {
        for (direction in listOf(0f, 90f, 180f, 270f)) {
            val filter = KalmanGPSModule(mock())
            val cached = reading(1).apply {
                rawBearing = direction
                speed = Speed.from(10f, DistanceUnits.Meters, TimeUnits.Seconds)
            }
            val expected = cached.location.plus(Distance.meters(20f), Bearing.from(direction))
            val next = reading(3).apply { location = expected }
            filter.update(cached, next)
            assertEquals(expected.latitude, next.location.latitude, 0.0000001)
            assertEquals(expected.longitude, next.location.longitude, 0.0000001)
        }
    }

    @Test
    fun missingBearingDoesNotAssumeNorthwardMotion() {
        val cached = reading(1).apply {
            speed = Speed.from(10f, DistanceUnits.Meters, TimeUnits.Seconds)
        }
        val next = reading(2)
        module.update(cached, next)
        assertEquals(cached.location, next.location)
    }

    @Test
    fun newVelocityIsUsedForFollowingFix() {
        val first = reading(1).apply {
            bearing = Bearing.from(90f)
            speed = Speed.from(10f, DistanceUnits.Meters, TimeUnits.Seconds)
        }
        module.update(previous, first)
        val second = reading(2).apply {
            location = first.location.plus(Distance.meters(10f), Bearing.from(90f))
        }
        module.update(first, second)
        val third = reading(3).apply { location = second.location }
        module.update(second, third)
        assertEquals(second.location, third.location)
    }

    @Test
    fun initializesFromCachedPreviousReading() {
        val cached = reading(1).apply { fixTimeElapsedNanos = null }
        val next = reading(2, 1.001)
        module.update(cached, next)

        val continuous = KalmanGPSModule(mock())
        continuous.update(previous, reading(1))
        val expected = reading(2, 1.001)
        continuous.update(previous, expected)
        assertEquals(expected.location, next.location)
        assertEquals(expected.horizontalAccuracy, next.horizontalAccuracy)
        assertEquals(Coordinate(1.0, 1.0), cached.location)
        assertEquals(10f, cached.horizontalAccuracy)
    }

    @Test
    fun cachedFixIsNotFilteredAgain() {
        val cached = reading(1).apply { fixTimeElapsedNanos = null }
        val duplicate = reading(1, 1.001).apply { time = time.plusNanos(123456) }
        module.update(cached, duplicate)
        assertEquals(cached.location, duplicate.location)
        assertEquals(cached.horizontalAccuracy, duplicate.horizontalAccuracy)
    }

    @Test
    fun resynchronizesWithNewerReadingFromAnotherInstance() {
        module.update(previous, reading(1))
        val other = KalmanGPSModule(mock())
        val cached = reading(2, 1.001)
        other.update(previous, cached)
        val newerCached = reading(3, 1.002)
        other.update(cached, newerCached)

        val next = reading(4, 1.003)
        val expected = reading(4, 1.003)
        other.update(newerCached, expected)
        module.update(newerCached, next)

        assertEquals(expected.location, next.location)
        // Restoring the Float accuracy loses precision compared with the internal Double variance.
        assertEquals(expected.horizontalAccuracy!!, next.horizontalAccuracy!!, 0.00001f)
    }

    @Test
    fun restoredNewerFixIsNotFilteredAgain() {
        module.update(previous, reading(1))
        val cached = reading(3, 1.002).apply { horizontalAccuracy = 4f }
        val duplicate = reading(3, 1.003).apply { time = time.plusNanos(123456) }
        module.update(cached, duplicate)
        assertEquals(cached.location, duplicate.location)
        assertEquals(cached.horizontalAccuracy, duplicate.horizontalAccuracy)
    }

    @Test
    fun ignoresPreviousReadingFromTheFuture() {
        val next = reading(1)
        assertTrue(module.update(reading(2, 1.001), next))
        assertEquals(Coordinate(1.0, 1.0), next.location)
        assertEquals(10f, next.horizontalAccuracy)
    }

    @Test
    fun smoothsPositionAndRetainsStateAcrossRestarts() {
        module.update(previous, reading(1))
        module.stop(previous)
        module.start(previous)
        val next = reading(2, 1.001)
        assertTrue(module.update(previous, next))
        assertTrue(next.location.longitude > 1.0 && next.location.longitude < 1.001)
        assertTrue(next.horizontalAccuracy!! < 10f)
        assertEquals(Coordinate.zero, previous.location)
    }

    @Test
    fun duplicateFixReusesEstimateWithoutReducingUncertainty() {
        module.update(previous, reading(1))
        val next = reading(2, 1.001)
        module.update(previous, next)
        repeat(10) {
            val duplicate = reading(2, 1.001)
            module.update(previous, duplicate)
            assertEquals(next.location, duplicate.location)
            assertEquals(next.horizontalAccuracy, duplicate.horizontalAccuracy)
        }
    }

    @Test
    fun deduplicatesByTimeWhenElapsedTimeIsUnavailable() {
        module.update(previous, reading(1).apply { fixTimeElapsedNanos = null })
        val duplicate = reading(1).apply { fixTimeElapsedNanos = null }
        module.update(previous, duplicate)
        assertEquals(10f, duplicate.horizontalAccuracy)
    }

    @Test
    fun identicalCoordinatesWithNewFixTimeAreNewMeasurements() {
        module.update(previous, reading(1))
        val next = reading(2)
        module.update(previous, next)
        assertTrue(next.horizontalAccuracy!! < 10f)
    }

    @Test
    fun resetsForOlderFixes() {
        module.update(previous, reading(2))
        val next = reading(1, 1.001)
        assertTrue(module.update(previous, next))
        assertEquals(Coordinate(1.0, 1.001), next.location)
        assertEquals(10f, next.horizontalAccuracy)
        val duplicate = reading(1, 1.001)
        module.update(previous, duplicate)
        assertEquals(next.location, duplicate.location)
        assertEquals(next.horizontalAccuracy, duplicate.horizontalAccuracy)
    }

    @Test
    fun resetsWhenNewTimePrecedesPreviousData() {
        module.update(previous, reading(1))
        val next = reading(2, 1.001)
        module.update(reading(3), next)
        assertEquals(Coordinate(1.0, 1.001), next.location)
        assertEquals(10f, next.horizontalAccuracy)
    }

    @Test
    fun usesTimeEvenWhenElapsedTimeMovesBackward() {
        module.update(previous, reading(1))
        val next = reading(2, 1.001).apply { fixTimeElapsedNanos = 0 }
        module.update(previous, next)
        assertTrue(next.location.longitude > 1.0 && next.location.longitude < 1.001)
        assertTrue(next.horizontalAccuracy!! < 10f)
    }

    @Test
    fun deduplicatesTimeEvenWhenElapsedTimeChanges() {
        module.update(previous, reading(1))
        val next = reading(1, 1.001).apply { fixTimeElapsedNanos = 2_000_000_000 }
        module.update(previous, next)
        assertEquals(Coordinate(1.0, 1.0), next.location)
        assertEquals(10f, next.horizontalAccuracy)
    }

    @Test
    fun uncertaintyGrowsDuringLongPause() {
        module.update(previous, reading(1))
        val next = reading(3601, 1.001)
        module.update(previous, next)
        assertEquals(1.001, next.location.longitude, 0.00001)
    }

    @Test
    fun handlesAntimeridianAndInvalidAccuracy() {
        module.update(previous, reading(1, 179.999))
        val next = reading(2, -179.999).apply { horizontalAccuracy = Float.NaN }
        module.update(previous, next)
        assertTrue(kotlin.math.abs(next.location.longitude) > 179.99)
        assertTrue(next.horizontalAccuracy!!.isFinite())
    }
}
