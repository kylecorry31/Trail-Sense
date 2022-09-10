package com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class NameBeaconSortTest {

    @Test
    fun sort() = runBlocking {

        val sort = NameBeaconSort()

        val beacons = listOf(
            Beacon(1, "Test 2", Coordinate.zero),
            Beacon(2, "Test 0", Coordinate.zero),
            BeaconGroup(3, "Test 1"),
        )

        val expected = listOf(
            beacons[1], beacons[2], beacons[0]
        )

        val sorted = sort.sort(beacons)

        assertEquals(expected, sorted)
    }

    @Test
    fun sortWhenEmpty() = runBlocking {
        val sort = NameBeaconSort()

        val sorted = sort.sort(emptyList())

        assertEquals(emptyList<IBeacon>(), sorted)
    }
}