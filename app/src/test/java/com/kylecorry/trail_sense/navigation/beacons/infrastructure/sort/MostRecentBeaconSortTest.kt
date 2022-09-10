package com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class MostRecentBeaconSortTest {

    @Test
    fun sort() = runBlocking {

        val beaconService = mock<IBeaconService>()
        val loader = mock<IGroupLoader<IBeacon>>()

        whenever(loader.getChildren(0, null)).thenReturn(listOf(
            beacon(4),
            beacon(5),
        ))

        whenever(beaconService.loader).thenReturn(loader)

        val paths = listOf(
            beacon(1),
            beacon(2),
            group(0),
            beacon(3)
        )

        val sort = MostRecentBeaconSort(beaconService)

        val sorted = sort.sort(paths).map { it.id }

        Assertions.assertEquals(listOf(0L, 3L, 2L, 1L), sorted)
    }

    private fun beacon(id: Long): Beacon {
        return Beacon(id, "", Coordinate.zero)
    }

    private fun group(id: Long): IBeacon {
        val group = mock<IBeacon>()
        whenever(group.id).thenReturn(id)
        whenever(group.isGroup).thenReturn(true)
        return group
    }

}