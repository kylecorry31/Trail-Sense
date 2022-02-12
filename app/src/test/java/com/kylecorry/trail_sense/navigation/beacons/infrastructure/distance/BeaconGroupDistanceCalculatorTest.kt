package com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.IBeaconService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class BeaconGroupDistanceCalculatorTest {

    @Test
    fun calculate() = runBlocking {
        // Arrange
        val service = mock<IBeaconService>()
        val factory = mock<IBeaconDistanceCalculatorFactory>()
        val calculator1 = mock<IBeaconDistanceCalculator<IBeacon>>()
        val calculator2 = mock<IBeaconDistanceCalculator<IBeacon>>()

        val beacons: List<IBeacon> = listOf(
            mock(),
            mock()
        )

        whenever(service.getBeacons(1))
            .thenReturn(beacons)

        whenever(factory.getCalculator(beacons[0]))
            .thenReturn(calculator1)

        whenever(factory.getCalculator(beacons[1]))
            .thenReturn(calculator2)

        whenever(calculator1.calculate(Coordinate.zero, beacons[0]))
            .thenReturn(10f)

        whenever(calculator2.calculate(Coordinate.zero, beacons[1]))
            .thenReturn(1f)

        val calculator = BeaconGroupDistanceCalculator(service, factory)

        // Act
        val distance = calculator.calculate(Coordinate.zero, BeaconGroup(1, ""))

        // Assert
        assertEquals(1f, distance, 0.0f)

    }
}