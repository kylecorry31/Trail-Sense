package com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence

import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader

interface IBeaconService {

    val loader: IGroupLoader<IBeacon>

    // Add
    suspend fun add(beacon: Beacon): Long
    suspend fun add(group: BeaconGroup): Long

    // Get
    suspend fun getBeacons(
        groupId: Long?,
        includeGroups: Boolean = true,
        maxDepth: Int? = 1,
        includeRoot: Boolean = false
    ): List<IBeacon>

    suspend fun getGroup(groupId: Long?): BeaconGroup?
    suspend fun getBeacon(beaconId: Long): Beacon?
    suspend fun getTemporaryBeacon(owner: BeaconOwner): Beacon?
    suspend fun search(
        nameFilter: String,
        groupFilter: Long?
    ): List<IBeacon>

    // Delete
    suspend fun delete(group: BeaconGroup)
    suspend fun delete(beacon: Beacon)
}