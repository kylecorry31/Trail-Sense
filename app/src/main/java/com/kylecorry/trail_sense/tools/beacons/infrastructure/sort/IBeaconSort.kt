package com.kylecorry.trail_sense.tools.beacons.infrastructure.sort

import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon

interface IBeaconSort {

    suspend fun sort(beacons: List<IBeacon>): List<IBeacon>

}