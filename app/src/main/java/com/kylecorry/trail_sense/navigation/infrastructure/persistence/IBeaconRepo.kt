package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.domain.BeaconGroupEntity
import com.kylecorry.trailsensecore.domain.navigation.BeaconOwner

interface IBeaconRepo {
    fun getBeacons(): LiveData<List<BeaconEntity>>
    suspend fun getBeaconsSync(): List<BeaconEntity>
    suspend fun searchBeacons(text: String): List<BeaconEntity>
    suspend fun searchBeaconsInGroup(text: String, groupId: Long?): List<BeaconEntity>
    suspend fun getBeaconsInGroup(groupId: Long?): List<BeaconEntity>
    suspend fun getBeacon(id: Long): BeaconEntity?
    suspend fun getTemporaryBeacon(owner: BeaconOwner): BeaconEntity?
    suspend fun deleteBeacon(beacon: BeaconEntity)
    suspend fun addBeacon(beacon: BeaconEntity): Long

    suspend fun addBeaconGroup(group: BeaconGroupEntity): Long
    suspend fun deleteBeaconGroup(group: BeaconGroupEntity)
    fun getGroups(): LiveData<List<BeaconGroupEntity>>
    suspend fun getGroupsSync(): List<BeaconGroupEntity>
    suspend fun getGroup(id: Long): BeaconGroupEntity?

}