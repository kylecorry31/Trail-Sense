package com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort

import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon

class AlphabeticalBeaconSort : IBeaconSort {
    override suspend fun sort(beacons: List<IBeacon>): List<IBeacon> {
        return beacons.sortedBy { it.name }
    }
}