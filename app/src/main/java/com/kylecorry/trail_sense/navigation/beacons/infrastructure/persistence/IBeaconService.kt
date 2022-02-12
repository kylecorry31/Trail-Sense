package com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence

import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon

interface IBeaconService {
    // Add
    suspend fun add(beacon: Beacon): Long

    // Get
    suspend fun getBeacons(
        groupId: Long?,
        includeGroups: Boolean = true,
        includeChildren: Boolean = false,
        includeParent: Boolean = false
    ): List<IBeacon>

    suspend fun getGroup(groupId: Long): BeaconGroup?
    suspend fun getBeacon(beaconId: Long): Beacon?
    suspend fun getTemporaryBeacon(owner: BeaconOwner): Beacon?
    suspend fun getBeaconCount(groupId: Long?): Int
    suspend fun search(
        nameFilter: String,
        groupFilter: Long?,
        applyGroupFilterIfNull: Boolean = false
    ): List<IBeacon>

    // Delete
    suspend fun delete(group: BeaconGroup)
    suspend fun delete(beacon: Beacon)
}