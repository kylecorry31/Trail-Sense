package com.kylecorry.trail_sense.tools.beacons.infrastructure

import com.kylecorry.trail_sense.tools.beacons.domain.Beacon

interface IBeaconNavigator {
    suspend fun navigateTo(beacon: Beacon)
}