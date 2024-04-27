package com.kylecorry.trail_sense.tools.beacons.infrastructure.sort

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.mappers.BeaconDistanceMapper

class ClosestBeaconSort(
    beaconService: IBeaconService,
    locationProvider: () -> Coordinate
) : IBeaconSort {

    private val sort = GroupSort(
        BeaconDistanceMapper(beaconService.loader, locationProvider)
    )

    override suspend fun sort(beacons: List<IBeacon>): List<IBeacon> {
        return sort.sort(beacons)
    }
}