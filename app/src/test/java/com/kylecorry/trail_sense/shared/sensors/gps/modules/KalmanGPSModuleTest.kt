package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class KalmanGPSModuleTest {
    private val prefs = mock<IGPSPreferences> {
        on { useFilteredGPS }.thenReturn(true)
    }
    private val module = KalmanGPSModule(prefs, mock())
    private val previous = ModularGPSData(time = Instant.EPOCH)

    private fun reading(seconds: Long, longitude: Double = 1.0) = ModularGPSData(
        location = Coordinate(1.0, longitude),
        time = Instant.EPOCH.plusSeconds(seconds),
        horizontalAccuracy = 10f,
        fixTimeElapsedNanos = seconds * 1_000_000_000
    )

    @Test
    fun invalidSpeedInitializesLikeMissingVelocity() = runBlocking<Unit> {
        for (speed in listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            val invalid = reading(1).apply {
                this.speed = Speed.from(speed, DistanceUnits.Meters, TimeUnits.Seconds)
                rawBearing = 90f
                speedAccuracy = 0.1f
            }
            val expected = reading(1)
            KalmanGPSModule(prefs, mock()).update(previous, expected)
            KalmanGPSModule(prefs, mock()).update(previous, invalid)
            assertEquals(expected.kalmanState, invalid.kalmanState, "speed: $speed")
        }
    }

    @Test
    fun invalidSpeedCorrectsLikeMissingVelocity() = runBlocking<Unit> {
        suspend fun correct(speed: Float, bearing: Float?): ModularGPSData {
            val filter = KalmanGPSModule(prefs, mock())
            val first = reading(1).apply {
                this.speed = Speed.from(10f, DistanceUnits.Meters, TimeUnits.Seconds)
                rawBearing = 90f
                speedAccuracy = 0.1f
            }
            filter.update(previous, first)
            return reading(2).apply {
                this.speed = Speed.from(speed, DistanceUnits.Meters, TimeUnits.Seconds)
                rawBearing = bearing
                speedAccuracy = 0.1f
                filter.update(first, this)
            }
        }

        val expected = correct(0f, null)
        for (speed in listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            val invalid = correct(speed, 90f)
            assertEquals(expected.location, invalid.location, "speed: $speed")
            assertEquals(expected.kalmanState, invalid.kalmanState, "speed: $speed")
        }
        // A valid zero speed must still be assimilated as a stationary measurement.
        assertNotEquals(expected.kalmanState, correct(0f, 90f).kalmanState)
    }

    @Test
    fun predictsUsingCachedVelocityInEachDirection() = runBlocking<Unit> {
        for (direction in listOf(0f, 90f, 180f, 270f)) {
            val filter = KalmanGPSModule(prefs, mock())
            val cached = reading(1).apply {
                rawBearing = direction
                speed = Speed.from(10f, DistanceUnits.Meters, TimeUnits.Seconds)
            }
            val expected = cached.location.plus(Distance.meters(20f), Bearing.from(direction))
            val next = reading(3).apply { location = expected }
            filter.update(cached, next)
            assertTrue(next.location.distanceTo(expected) < 1f)
        }
    }

    @Test
    fun missingBearingDoesNotAssumeNorthwardMotion() = runBlocking<Unit> {
        val cached = reading(1).apply {
            speed = Speed.from(10f, DistanceUnits.Meters, TimeUnits.Seconds)
        }
        val next = reading(2)
        module.update(cached, next)
        assertEquals(cached.location, next.location)
    }

    @Test
    fun newVelocityIsUsedForFollowingFix() = runBlocking<Unit> {
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
        assertTrue(third.location.distanceTo(second.location) < 10f)
    }

    @Test
    fun initializesFromCachedPreviousReading() = runBlocking<Unit> {
        val cached = reading(1).apply { fixTimeElapsedNanos = null }
        val next = reading(2, 1.001)
        module.update(cached, next)

        val continuous = KalmanGPSModule(prefs, mock())
        continuous.update(previous, reading(1))
        val expected = reading(2, 1.001)
        continuous.update(previous, expected)
        assertEquals(expected.location, next.location)
        assertEquals(expected.horizontalAccuracy, next.horizontalAccuracy)
        assertEquals(Coordinate(1.0, 1.0), cached.location)
        assertEquals(10f, cached.horizontalAccuracy)
    }

    @Test
    fun cachedFixIsNotFilteredAgain() = runBlocking<Unit> {
        val cached = reading(1).apply { fixTimeElapsedNanos = null }
        val duplicate = reading(1, 1.001).apply { time = time.plusNanos(123456) }
        module.update(cached, duplicate)
        assertEquals(cached.location, duplicate.location)
        assertEquals(cached.horizontalAccuracy, duplicate.horizontalAccuracy)
    }

    @Test
    fun resynchronizesWithNewerReadingFromAnotherInstance() = runBlocking<Unit> {
        module.update(previous, reading(1))
        val other = KalmanGPSModule(prefs, mock())
        val cached = reading(2, 1.001)
        other.update(previous, cached)
        val newerCached = reading(3, 1.002)
        other.update(cached, newerCached)

        val next = reading(4, 1.003)
        val expected = reading(4, 1.003)
        KalmanGPSModule(prefs, mock()).update(newerCached, expected)
        module.update(newerCached, next)

        // Restore the internal covariance independently of the reported accuracy.
        assertEquals(expected.location, next.location)
        // Both instances preserve the reported measurement accuracy.
        assertEquals(expected.horizontalAccuracy!!, next.horizontalAccuracy!!, 0.00001f)
    }

    @Test
    fun restoredNewerFixIsNotFilteredAgain() = runBlocking<Unit> {
        module.update(previous, reading(1))
        val cached = reading(3, 1.002).apply { horizontalAccuracy = 4f }
        val duplicate = reading(3, 1.003).apply { time = time.plusNanos(123456) }
        module.update(cached, duplicate)
        assertEquals(cached.location, duplicate.location)
        assertEquals(cached.horizontalAccuracy, duplicate.horizontalAccuracy)
    }

    @Test
    fun ignoresPreviousReadingFromTheFuture() = runBlocking<Unit> {
        val next = reading(1)
        assertTrue(module.update(reading(2, 1.001), next))
        assertEquals(Coordinate(1.0, 1.0), next.location)
        assertEquals(10f, next.horizontalAccuracy)
    }

    @Test
    fun smoothsPositionAndRetainsStateAcrossRestarts() = runBlocking<Unit> {
        module.update(previous, reading(1))
        module.stop(previous)
        module.start(previous)
        val next = reading(2, 1.001)
        assertTrue(module.update(previous, next))
        assertTrue(next.location.longitude > 1.0 && next.location.longitude < 1.001)
        assertEquals(10f, next.horizontalAccuracy)
        assertEquals(Coordinate.zero, previous.location)
    }

    @Test
    fun duplicateFixReusesEstimateWithoutReducingUncertainty() = runBlocking<Unit> {
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
    fun deduplicatesByTimeWhenElapsedTimeIsUnavailable() = runBlocking<Unit> {
        module.update(previous, reading(1).apply { fixTimeElapsedNanos = null })
        val duplicate = reading(1).apply { fixTimeElapsedNanos = null }
        module.update(previous, duplicate)
        assertEquals(10f, duplicate.horizontalAccuracy)
    }

    @Test
    fun identicalCoordinatesWithNewFixTimeAreNewMeasurements() = runBlocking<Unit> {
        module.update(previous, reading(1))
        val next = reading(2)
        module.update(previous, next)
        assertEquals(10f, next.horizontalAccuracy)
    }

    @Test
    fun resetsForOlderFixes() = runBlocking<Unit> {
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
    fun resetsWhenNewTimePrecedesPreviousData() = runBlocking<Unit> {
        module.update(previous, reading(1))
        val next = reading(2, 1.001)
        module.update(reading(3), next)
        assertEquals(Coordinate(1.0, 1.001), next.location)
        assertEquals(10f, next.horizontalAccuracy)
    }

    @Test
    fun republishedFixReportsTheEstimateWhileRunningAheadOfTheAcceptedReading() =
        runBlocking<Unit> {
            val accepted = acceptFirstFix(module)
            val absorbed = reading(2, 1.001)
            module.update(accepted, absorbed)

            val republished = reading(1, 1.5)
            assertTrue(module.update(accepted, republished))
            assertEquals(absorbed.location, republished.location)
        }

    @Test
    fun republishedFixLeavesTheFilterRunningAheadUnchanged() = runBlocking<Unit> {
        suspend fun advance(module: KalmanGPSModule, republish: Boolean): ModularGPSData {
            val accepted = acceptFirstFix(module)
            // Absorbed, then rejected downstream, so the accepted reading stays behind the filter.
            module.update(accepted, reading(2, 1.001))
            if (republish) {
                module.update(accepted, reading(1, 1.5))
            }
            return reading(3, 1.002).also { module.update(accepted, it) }
        }

        val withRepublish = advance(KalmanGPSModule(prefs, mock()), true)
        val without = advance(KalmanGPSModule(prefs, mock()), false)
        assertEquals(without.location, withRepublish.location)
        assertEquals(without.horizontalAccuracy, withRepublish.horizontalAccuracy)
    }

    private suspend fun acceptFirstFix(module: KalmanGPSModule): ModularGPSData {
        val accepted = ModularGPSData(time = Instant.EPOCH)
        val published = reading(1)
        module.update(accepted, published)
        published.copyInto(accepted)
        return accepted
    }

    @Test
    fun usesTimeEvenWhenElapsedTimeMovesBackward() = runBlocking<Unit> {
        module.update(previous, reading(1))
        val next = reading(2, 1.001).apply { fixTimeElapsedNanos = 0 }
        module.update(previous, next)
        assertTrue(next.location.longitude > 1.0 && next.location.longitude < 1.001)
        assertEquals(10f, next.horizontalAccuracy)
    }

    @Test
    fun deduplicatesTimeEvenWhenElapsedTimeChanges() = runBlocking<Unit> {
        module.update(previous, reading(1))
        val next = reading(1, 1.001).apply { fixTimeElapsedNanos = 2_000_000_000 }
        module.update(previous, next)
        assertEquals(Coordinate(1.0, 1.0), next.location)
        assertEquals(10f, next.horizontalAccuracy)
    }

    @Test
    fun uncertaintyGrowsDuringLongPause() = runBlocking<Unit> {
        module.update(previous, reading(1))
        val next = reading(3601, 1.001)
        module.update(previous, next)
        assertEquals(1.001, next.location.longitude, 0.00001)
    }

    @Test
    fun fifteenSecondIntervalStillUsesThePreviousEstimate() = runBlocking<Unit> {
        val first = reading(1)
        module.update(previous, first)
        val next = reading(16, 1.001)
        module.update(first, next)
        assertTrue(next.location.longitude > first.location.longitude)
        assertTrue(next.location.longitude < 1.001)
    }

    @Test
    fun supportsVariableBacktrackIntervalsAcrossStopsAndRestarts() = runBlocking<Unit> {
        val last = reading(1).apply {
            speed = Speed.from(30f, DistanceUnits.Meters, TimeUnits.Seconds)
            rawBearing = 90f
        }
        module.update(previous, last)
        for (interval in listOf(15L, 900L, 1800L, 3600L, 86400L, 1L)) {
            module.stop(last)
            module.start(last)
            val next = reading(last.time.epochSecond + interval, 1.001).apply {
                speed = last.speed
                rawBearing = last.rawBearing
            }
            val measurement = next.location
            module.update(last, next)
            // The old velocity must not drag a sparse fix far from the new measurement.
            assertTrue(next.location.distanceTo(measurement) < 20f, "interval: $interval")
            assertTrue(next.horizontalAccuracy!!.isFinite())
            val duplicate = reading(next.time.epochSecond, 1.001)
            module.update(last, duplicate)
            assertEquals(next.location, duplicate.location)
            next.copyInto(last)
        }
    }

    @Test
    fun longGapRetainsMeasurementUncertaintyForFollowingFix() = runBlocking<Unit> {
        module.update(previous, reading(1))
        val sparse = reading(1 + 365L * 86400, 1.001)
        module.update(previous, sparse)
        val next = reading(sparse.time.epochSecond + 1, 1.002)
        module.update(sparse, next)

        val fresh = KalmanGPSModule(prefs, mock())
        val expected = reading(next.time.epochSecond, 1.002)
        fresh.update(sparse, expected)
        assertTrue(expected.location.distanceTo(next.location) < 0.01f)
    }

    @Test
    fun reducesStationaryNoiseAtOneHertz() = runBlocking<Unit> {
        val truth = Coordinate(1.0, 1.0)
        val random = java.util.Random(42)
        var rawSquaredError = 0.0
        var filteredSquaredError = 0.0
        val last = ModularGPSData(time = Instant.EPOCH)
        repeat(300) { index ->
            val next = reading(index.toLong() + 1).apply {
                location = truth.plus(
                    Distance.meters((random.nextGaussian() * 10).toFloat()),
                    Bearing.from(random.nextFloat() * 360)
                )
            }
            val rawError = truth.distanceTo(next.location).toDouble()
            module.update(last, next)
            if (index > 30) {
                rawSquaredError += rawError * rawError
                val filteredError = truth.distanceTo(next.location).toDouble()
                filteredSquaredError += filteredError * filteredError
            }
            next.copyInto(last)
        }
        assertTrue(filteredSquaredError < rawSquaredError * 0.6)
    }

    @Test
    fun tracksDrivingAtOneHertzWithoutAccumulatingLag() = runBlocking<Unit> {
        val origin = Coordinate(1.0, 1.0)
        val last = ModularGPSData(time = Instant.EPOCH)
        repeat(120) { index ->
            val truth = origin.plus(Distance.meters(index * 25f), Bearing.from(90f))
            val next = reading(index.toLong() + 1).apply {
                location = truth
                speed = Speed.from(25f, DistanceUnits.Meters, TimeUnits.Seconds)
                rawBearing = 90f
                speedAccuracy = 1f
                bearingAccuracy = 3f
            }
            module.update(last, next)
            val error = truth.distanceTo(next.location)
            assertTrue(error < 3f, "index: $index, error: $error")
            next.copyInto(last)
        }
    }

    @Test
    fun uncertainVelocityTrustsPositionMoreThanPreciseVelocity() = runBlocking<Unit> {
        suspend fun filtered(error: Float): ModularGPSData {
            val filter = KalmanGPSModule(prefs, mock())
            val first = reading(1).apply {
                speed = Speed.from(20f, DistanceUnits.Meters, TimeUnits.Seconds)
                rawBearing = 90f
                speedAccuracy = error
                bearingAccuracy = 1f
            }
            filter.update(previous, first)
            return reading(2).also { filter.update(first, it) }
        }
        assertTrue(filtered(20f).location.distanceTo(reading(2).location) <
            filtered(1f).location.distanceTo(reading(2).location))
    }

    @Test
    fun handlesAntimeridianAndInvalidAccuracy() = runBlocking<Unit> {
        module.update(previous, reading(1, 179.999))
        val next = reading(2, -179.999).apply { horizontalAccuracy = Float.NaN }
        module.update(previous, next)
        assertTrue(kotlin.math.abs(next.location.longitude) > 179.99)
        assertTrue(next.horizontalAccuracy!!.isFinite())
    }
}
