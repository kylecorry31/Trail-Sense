package com.kylecorry.trail_sense.navigation.beacons.infrastructure

import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon

interface IBeaconNavigator {
    suspend fun navigateTo(beacon: Beacon)
}