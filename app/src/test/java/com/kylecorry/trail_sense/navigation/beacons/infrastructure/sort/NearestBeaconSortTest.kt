package com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance.IBeaconDistanceCalculator
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance.IBeaconDistanceCalculatorFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class NearestBeaconSortTest {

    private lateinit var factory: IBeaconDistanceCalculatorFactory

    @Test
    fun sort() = runBlocking {
        factory = mock()
        val location = Coordinate(10.0, 5.0)
        val sort = NearestBeaconSort(factory) { location }

        val beacons = listOf(
            Beacon(1, "Test", Coordinate.zero),
            Beacon(2, "Test", Coordinate.zero),
            BeaconGroup(3, "Test"),
        )

        setupCalculator(location, beacons[0], 100f)
        setupCalculator(location, beacons[1], 10f)
        setupCalculator(location, beacons[2], 20f)

        val expected = listOf(
            beacons[1], beacons[2], beacons[0]
        )

        val sorted = sort.sort(beacons)

        assertEquals(expected, sorted)
    }

    @Test
    fun sortWhenEmpty() = runBlocking {
        val sort = NearestBeaconSort(mock()) { Coordinate.zero }

        val sorted = sort.sort(emptyList())

        assertEquals(emptyList<IBeacon>(), sorted)
    }


    private fun <T : IBeacon> setupCalculator(
        location: Coordinate,
        beacon: T,
        distance: Float
    ) = runBlocking {
        val calculator = mock<IBeaconDistanceCalculator<T>>()

        whenever(calculator.calculate(location, beacon))
            .thenReturn(distance)

        whenever(factory.getCalculator(beacon))
            .thenReturn(calculator)
    }
}