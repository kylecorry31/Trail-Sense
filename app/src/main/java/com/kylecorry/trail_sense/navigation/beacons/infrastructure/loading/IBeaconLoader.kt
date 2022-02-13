package com.kylecorry.trail_sense.navigation.beacons.infrastructure.loading

import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon

interface IBeaconLoader {
    suspend fun load(search: String?, group: Long?): List<IBeacon>
}