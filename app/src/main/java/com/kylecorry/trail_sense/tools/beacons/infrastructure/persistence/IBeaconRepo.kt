package com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import kotlinx.coroutines.flow.Flow

interface IBeaconRepo {
    fun getBeacons(): Flow<List<Beacon>>
    suspend fun getBeaconsSync(): List<BeaconEntity>
    suspend fun searchBeacons(text: String): List<BeaconEntity>
    suspend fun searchBeaconsInGroup(text: String, groupId: Long?): List<BeaconEntity>
    suspend fun getBeaconsInGroup(groupId: Long?): List<BeaconEntity>
    suspend fun getBeacon(id: Long): BeaconEntity?
    suspend fun getTemporaryBeacon(owner: BeaconOwner): BeaconEntity?
    suspend fun deleteBeacon(beacon: BeaconEntity)
    suspend fun addBeacon(beacon: BeaconEntity): Long

    suspend fun addBeaconGroup(group: BeaconGroupEntity): Long
    suspend fun deleteBeaconGroup(group: BeaconGroupEntity?)
    suspend fun getGroupsWithParent(parent: Long?): List<BeaconGroupEntity>
    suspend fun getGroup(id: Long): BeaconGroupEntity?

    suspend fun getBeaconsInRegion(region: CoordinateBounds): List<BeaconEntity>

}