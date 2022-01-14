package com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence

import android.content.Context
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon

class BeaconService(private val context: Context) {

    private val repo = BeaconRepo.getInstance(context)

    suspend fun addBeacon(beacon: Beacon): Long {
        return repo.addBeacon(BeaconEntity.from(beacon))
    }

}