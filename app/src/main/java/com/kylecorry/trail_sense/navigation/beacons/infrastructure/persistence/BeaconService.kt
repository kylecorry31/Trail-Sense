package com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence

import android.content.Context
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon

class BeaconService(context: Context) : IBeaconService {

    private val repo = BeaconRepo.getInstance(context)

    override suspend fun add(beacon: Beacon): Long {
        if (beacon.id == 0L && beacon.temporary) {
            val id = getTemporaryBeacon(beacon.owner)?.id ?: 0L
            return repo.addBeacon(BeaconEntity.from(beacon.copy(id = id)))
        }
        return repo.addBeacon(BeaconEntity.from(beacon))
    }

    override suspend fun getBeacons(groupId: Long?, includeGroups: Boolean): List<IBeacon> {
        val beacons = getBeaconsWithParent(groupId)
        val groups = if (includeGroups) getGroupsWithParent(groupId) else emptyList()
        return beacons + groups
    }

    private suspend fun getBeaconsWithParent(groupId: Long?): List<Beacon> {
        return repo.getBeaconsInGroup(groupId).map { it.toBeacon() }
    }

    private suspend fun getGroupsWithParent(groupId: Long?): List<BeaconGroup> {
        return if (groupId == null) {
            repo.getGroupsSync().map { it.toBeaconGroup() }
        } else {
            emptyList()
        }
    }

    override suspend fun getTemporaryBeacon(owner: BeaconOwner): Beacon? {
        return repo.getTemporaryBeacon(owner)?.toBeacon()
    }

    override suspend fun getBeaconCount(groupId: Long?): Int {
        // TODO: Don't actually fetch the beacons for this
        return getBeaconsWithParent(groupId).count()
    }

    override suspend fun delete(group: BeaconGroup) {
        repo.deleteBeaconGroup(BeaconGroupEntity.from(group))
    }

    override suspend fun delete(beacon: Beacon) {
        repo.deleteBeacon(BeaconEntity.from(beacon))
    }

}