package com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence

import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon

interface IBeaconService {
    suspend fun addBeacon(beacon: Beacon): Long
}