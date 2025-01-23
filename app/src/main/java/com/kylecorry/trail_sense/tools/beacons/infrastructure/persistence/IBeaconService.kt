package com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon

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

    suspend fun getBeaconsInRegion(region: CoordinateBounds): List<Beacon>

    // Delete
    suspend fun delete(group: BeaconGroup?)
    suspend fun delete(beacon: Beacon)
}