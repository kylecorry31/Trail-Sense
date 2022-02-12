package com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BeaconDistanceCalculatorTest {

    @Test
    fun calculate() = runBlocking {
        // Arrange
        val beacon = Beacon(1, "", Coordinate(1.0, 2.0))
        val calculator = BeaconDistanceCalculator()

        val expected = Coordinate.zero.distanceTo(beacon.coordinate)

        // Act
        val distance = calculator.calculate(Coordinate.zero, beacon)

        // Assert
        assertEquals(expected, distance, 0.01f)

    }
}