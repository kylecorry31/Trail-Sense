package com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.mappers.BeaconDistanceMapper
import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort

class NearestBeaconSort(
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