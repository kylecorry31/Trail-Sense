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

    override suspend fun add(group: BeaconGroup): Long {
        return repo.addBeaconGroup(BeaconGroupEntity.from(group))
    }

    override suspend fun getBeacons(
        groupId: Long?,
        includeGroups: Boolean,
        includeChildren: Boolean,
        includeRoot: Boolean
    ): List<IBeacon> {
        val all = mutableListOf<IBeacon>()
        val beacons = getBeaconsWithParent(groupId)
        val groups =
            if (includeGroups || includeChildren) getGroups(groupId) else emptyList()

        val root = if (includeRoot && groupId != null) {
            getGroup(groupId)
        } else {
            null
        }

        all.addAll(beacons)

        if (root != null) {
            all.add(root)
        }

        if (includeGroups) {
            all.addAll(groups)
        }

        if (includeChildren) {
            for (group in groups) {
                all.addAll(
                    getBeacons(
                        group.id,
                        includeGroups,
                        includeChildren = true,
                        includeRoot = false
                    )
                )
            }
        }

        return all
    }

    override suspend fun getGroup(groupId: Long): BeaconGroup? {
        return repo.getGroup(groupId)?.toBeaconGroup()
    }

    override suspend fun getBeacon(beaconId: Long): Beacon? {
        return repo.getBeacon(beaconId)?.toBeacon()
    }

    private suspend fun getBeaconsWithParent(groupId: Long?): List<Beacon> {
        return repo.getBeaconsInGroup(groupId).map { it.toBeacon() }
    }

    override suspend fun getGroups(parent: Long?): List<BeaconGroup> {
        return repo.getGroupsWithParent(parent).map { it.toBeaconGroup() }
    }

    override suspend fun getTemporaryBeacon(owner: BeaconOwner): Beacon? {
        return repo.getTemporaryBeacon(owner)?.toBeacon()
    }

    override suspend fun getBeaconCount(groupId: Long?): Int {
        // TODO: Don't actually fetch the beacons for this
        return getBeacons(groupId, includeGroups = false, includeChildren = true).count()
    }

    override suspend fun search(
        nameFilter: String,
        groupFilter: Long?,
        applyGroupFilterIfNull: Boolean
    ): List<IBeacon> {
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