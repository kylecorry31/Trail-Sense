package com.kylecorry.trail_sense.tools.beacons.infrastructure.sort

import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.mappers.BeaconIdMapper

class MostRecentBeaconSort(beaconService: IBeaconService) : IBeaconSort {

    private val sort = GroupSort(BeaconIdMapper(beaconService.loader), ascending = false)

    override suspend fun sort(beacons: List<IBeacon>): List<IBeacon> {
        return sort.sort(beacons)
    }
}