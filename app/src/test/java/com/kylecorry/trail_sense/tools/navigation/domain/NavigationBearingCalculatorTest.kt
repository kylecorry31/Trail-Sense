package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

internal class NavigationBearingCalculatorTest {

    private val origin = Coordinate(0.0, 0.0)

    @ParameterizedTest
    @CsvSource(
        // True north keeps the bearing to the beacon
        "true, 0",
        // Magnetic north subtracts the 10 degree declination, wrapping past north
        "false, 350"
    )
    fun getBearingToBeacon(useTrueNorth: Boolean, expected: Float) {
        val destination = Destination.Beacon(Beacon.temporary(Coordinate(1.0, 0.0)))
        val bearing = NavigationBearingCalculator.getBearing(
            origin, destination, 10f, useTrueNorth
        )
        assertEquals(expected, bearing.value, 0.01f)
    }

    @ParameterizedTest
    @CsvSource(
        // The target is 1 km east of the start, seen from 1 degree north of it
        "true, 179.49",
        // The same bearing with the 10 degree declination subtracted
        "false, 169.49"
    )
    fun getBearingToLockedBearingPointsFromCurrentLocationToTarget(
        useTrueNorth: Boolean,
        expected: Float
    ) {
        val destination = Destination.Bearing(
            Bearing.from(90f),
            isTrueNorth = true,
            declination = 0f,
            startingLocation = origin,
            bearingDistance = Distance.kilometers(1f).meters()
        )
        val bearing = NavigationBearingCalculator.getBearing(
            Coordinate(1.0, 0.0), destination, 10f, useTrueNorth
        )
        assertEquals(expected, bearing.value, 0.1f)
    }

    @ParameterizedTest
    @CsvSource(
        // Without a starting location the recorded bearing is returned as is
        "true",
        "false"
    )
    fun getBearingToRecordedBearingWithoutStartingLocationReturnsRecordedBearing(
        useTrueNorth: Boolean
    ) {
        val destination = Destination.Bearing(
            Bearing.from(90f),
            isTrueNorth = false,
            declination = 10f,
            startingLocation = null
        )
        val bearing = NavigationBearingCalculator.getBearing(
            origin, destination, 10f, useTrueNorth
        )
        assertEquals(90f, bearing.value, 0.01f)
    }

    @ParameterizedTest
    @CsvSource(
        // The route runs due east, so the target is due east of the start
        "true, 90",
        // Magnetic north subtracts the 10 degree declination
        "false, 80"
    )
    fun getBearingToPathPointsAlongRouteFromCurrentLocation(useTrueNorth: Boolean, expected: Float) {
        val points = listOf(
            PathPoint(1, 1, Coordinate(0.0, 0.0), 0f),
            PathPoint(2, 1, Coordinate(0.0, 0.002), 0f)
        )
        val path = Path(1, null, PathStyle.default(), PathMetadata.empty)
        val destination = Destination.Path(path, points)
        destination.route.navigate(origin, null)
        val bearing = NavigationBearingCalculator.getBearing(
            origin, destination, 10f, useTrueNorth
        )
        assertEquals(expected, bearing.value, 0.1f)
    }

    @Test
    fun getDeclinationWithAutoDeclinationReturnsMagneticModelDeclination() {
        // Value from Sol's WMM2025 model for New York on 2026-06-01
        val declination = NavigationBearingCalculator.getDeclination(
            Coordinate(40.7128, -74.0060),
            elevation = 0f,
            time = Instant.parse("2026-06-01T00:00:00Z").toEpochMilli(),
            useAutoDeclination = true,
            declinationOverride = 5f
        )
        assertEquals(-12.477813f, declination, 0.001f)
    }

    @Test
    fun getDeclinationWithoutAutoDeclinationReturnsOverride() {
        val declination = NavigationBearingCalculator.getDeclination(
            origin,
            elevation = 0f,
            time = 0L,
            useAutoDeclination = false,
            declinationOverride = 5f
        )
        assertEquals(5f, declination, 0.0f)
    }
}
