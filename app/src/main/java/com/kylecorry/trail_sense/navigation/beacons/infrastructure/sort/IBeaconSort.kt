package com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort

import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon

interface IBeaconSort {

    suspend fun sort(beacons: List<IBeacon>): List<IBeacon>

}