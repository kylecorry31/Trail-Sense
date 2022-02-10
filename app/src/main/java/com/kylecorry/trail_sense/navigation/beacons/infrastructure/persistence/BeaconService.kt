package com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence

import android.content.Context
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner

class BeaconService(context: Context) : IBeaconService {

    private val repo = BeaconRepo.getInstance(context)

    override suspend fun addBeacon(beacon: Beacon): Long {
        if (beacon.id == 0L && beacon.temporary) {
            val id = getTemporaryBeacon(beacon.owner)?.id ?: 0L
            return repo.addBeacon(BeaconEntity.from(beacon.copy(id = id)))
        }
        return repo.addBeacon(BeaconEntity.from(beacon))
    }

    private suspend fun getTemporaryBeacon(owner: BeaconOwner): Beacon? {
        return repo.getTemporaryBeacon(owner)?.toBeacon()
    }

}