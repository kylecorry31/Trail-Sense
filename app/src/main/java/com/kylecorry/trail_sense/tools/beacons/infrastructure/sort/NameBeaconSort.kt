package com.kylecorry.trail_sense.tools.beacons.infrastructure.sort

import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.mappers.BeaconNameMapper

class NameBeaconSort : IBeaconSort {
    private val sort = GroupSort(BeaconNameMapper())

    override suspend fun sort(beacons: List<IBeacon>): List<IBeacon> {
        return sort.sort(beacons)
    }
}