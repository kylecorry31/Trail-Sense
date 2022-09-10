package com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort

import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.mappers.BeaconNameMapper
import com.kylecorry.trail_sense.shared.grouping.sort.GroupSort

class NameBeaconSort : IBeaconSort {
    private val sort = GroupSort(BeaconNameMapper())

    override suspend fun sort(beacons: List<IBeacon>): List<IBeacon> {
        return sort.sort(beacons)
    }
}