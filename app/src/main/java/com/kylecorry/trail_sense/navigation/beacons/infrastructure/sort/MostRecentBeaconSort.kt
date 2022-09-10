package com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort

import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.mappers.BeaconIdMapper
import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort

class MostRecentBeaconSort(beaconService: IBeaconService) : IBeaconSort {

    private val sort = GroupSort(BeaconIdMapper(beaconService.loader), ascending = false)

    override suspend fun sort(beacons: List<IBeacon>): List<IBeacon> {
        return sort.sort(beacons)
    }
}