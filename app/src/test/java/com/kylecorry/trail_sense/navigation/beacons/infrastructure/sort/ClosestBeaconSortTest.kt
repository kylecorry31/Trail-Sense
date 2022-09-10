package com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class ClosestBeaconSortTest {

    @Test
    fun sort() = runBlocking {

        val beaconService = mock<IBeaconService>()
        val loader = mock<IGroupLoader<IBeacon>>()

        whenever(loader.getChildren(6, null)).thenReturn(listOf(
            beacon(4, Coordinate(1.0, 2.0)),
            beacon(5, Coordinate(1.0, 1.0)),
        ))

        whenever(beaconService.loader).thenReturn(loader)

        val beacons = listOf(
            group(6),
            beacon(1, Coordinate(1.0, 0.0)),
            beacon(2, Coordinate(0.0, 0.0)),
            beacon(3, Coordinate(0.0, 1.0))
        )

        val sort = ClosestBeaconSort(beaconService){ Coordinate.zero }

        val sorted = sort.sort(beacons).map { it.id }

        assertEquals(listOf(2L, 1L, 3L, 6L), sorted)
    }

    private fun beacon(id: Long, center: Coordinate): Beacon {
        return Beacon(id, "", center)
    }

    private fun group(id: Long): IBeacon {
        val group = mock<IBeacon>()
        whenever(group.id).thenReturn(id)
        whenever(group.isGroup).thenReturn(true)
        return group
    }

}