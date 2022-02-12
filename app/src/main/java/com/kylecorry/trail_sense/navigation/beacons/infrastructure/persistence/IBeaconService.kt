package com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence

import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon

interface IBeaconService {
    // Add
    suspend fun add(beacon: Beacon): Long

    // Get
    suspend fun getBeacons(groupId: Long?, includeGroups: Boolean = true): List<IBeacon>
    suspend fun getTemporaryBeacon(owner: BeaconOwner): Beacon?
    suspend fun getBeaconCount(groupId: Long?): Int

    // Delete
    suspend fun delete(group: BeaconGroup)
    suspend fun delete(beacon: Beacon)
}