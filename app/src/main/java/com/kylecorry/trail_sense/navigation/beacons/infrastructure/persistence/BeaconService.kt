package com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence

import android.content.Context
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.grouping.GroupLoader

class BeaconService(context: Context) : IBeaconService {

    private val repo = BeaconRepo.getInstance(context)

    override suspend fun add(beacon: Beacon): Long {
        if (beacon.id == 0L && beacon.temporary) {
            val id = getTemporaryBeacon(beacon.owner)?.id ?: 0L
            return repo.addBeacon(BeaconEntity.from(beacon.copy(id = id)))
        }
        return repo.addBeacon(BeaconEntity.from(beacon))
    }

    override suspend fun add(group: BeaconGroup): Long {
        return repo.addBeaconGroup(BeaconGroupEntity.from(group))
    }


    override suspend fun getBeacons(
        groupId: Long?,
        includeGroups: Boolean,
        maxDepth: Int?,
        includeRoot: Boolean
    ): List<IBeacon> {
        val rootFn = if (includeRoot) {
            this::getGroup
        } else {
            { null }
        }

        val loader = GroupLoader(rootFn, this::getChildren)
        return onIO {
            val beacons = loader.load(groupId, maxDepth)
            if (includeGroups) {
                beacons
            } else {
                beacons.filterNot { it.isGroup }
            }
        }
    }

    private suspend fun getChildren(groupId: Long?): List<IBeacon> {
        val beacons = getBeaconsWithParent(groupId)
        val groups = getGroups(groupId)
        return beacons + groups
    }

    override suspend fun getGroup(groupId: Long?): BeaconGroup? {
        groupId ?: return null
        return repo.getGroup(groupId)?.toBeaconGroup()?.copy(count = getBeaconCount(groupId))
    }

    override suspend fun getBeacon(beaconId: Long): Beacon? {
        return repo.getBeacon(beaconId)?.toBeacon()
    }

    private suspend fun getBeaconsWithParent(groupId: Long?): List<Beacon> {
        return repo.getBeaconsInGroup(groupId).map { it.toBeacon() }
    }

    override suspend fun getGroups(parent: Long?): List<BeaconGroup> {
        return repo.getGroupsWithParent(parent).map { it.toBeaconGroup().copy(count = getBeaconCount(it.id)) }
    }

    override suspend fun getTemporaryBeacon(owner: BeaconOwner): Beacon? {
        return repo.getTemporaryBeacon(owner)?.toBeacon()
    }

    private suspend fun getBeaconCount(groupId: Long?): Int {
        // TODO: Don't actually fetch the beacons for this
        return getBeacons(groupId, includeGroups = false, maxDepth = null).count()
    }

    override suspend fun search(
        nameFilter: String,
        groupFilter: Long?,
        applyGroupFilterIfNull: Boolean
    ): List<IBeacon> {
        // TODO: Search sub groups
        return if (groupFilter != null || applyGroupFilterIfNull) {
            repo.searchBeaconsInGroup(
                nameFilter,
                groupFilter
            )
        } else {
            repo.searchBeacons(nameFilter)
        }.map { it.toBeacon() }
    }

    override suspend fun delete(group: BeaconGroup) {
        repo.deleteBeaconGroup(BeaconGroupEntity.from(group))
    }

    override suspend fun delete(beacon: Beacon) {
        repo.deleteBeacon(BeaconEntity.from(beacon))
    }

}