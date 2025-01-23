package com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.grouping.count.GroupCounter
import com.kylecorry.trail_sense.shared.grouping.filter.GroupFilter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupLoader
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon

class BeaconService(context: Context) : IBeaconService {

    private val repo = BeaconRepo.getInstance(context)
    override val loader = GroupLoader(this::getGroup, this::getChildren)
    private val counter = GroupCounter(loader)
    private val filter = GroupFilter(loader)


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
        return onIO {
            val root = listOfNotNull(
                if (includeRoot) {
                    loader.getGroup(groupId)
                } else {
                    null
                }
            )

            val beacons = root + loader.getChildren(groupId, maxDepth)
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
        return repo.getGroup(groupId)?.toBeaconGroup()?.copy(count = counter.count(groupId))
    }

    override suspend fun getBeacon(beaconId: Long): Beacon? {
        return repo.getBeacon(beaconId)?.toBeacon()
    }

    private suspend fun getBeaconsWithParent(groupId: Long?): List<Beacon> {
        return repo.getBeaconsInGroup(groupId).map { it.toBeacon() }
    }

    private suspend fun getGroups(parent: Long?): List<BeaconGroup> {
        return repo.getGroupsWithParent(parent)
            .map { it.toBeaconGroup().copy(count = counter.count(it.id)) }
    }

    override suspend fun getTemporaryBeacon(owner: BeaconOwner): Beacon? {
        return repo.getTemporaryBeacon(owner)?.toBeacon()
    }

    override suspend fun search(nameFilter: String, groupFilter: Long?): List<IBeacon> {
        return filter.filter(groupFilter) {
            it.name.contains(nameFilter, ignoreCase = true)
        }
    }

    override suspend fun getBeaconsInRegion(region: CoordinateBounds): List<Beacon> {
        return repo.getBeaconsInRegion(region).map { it.toBeacon() }
    }

    override suspend fun delete(group: BeaconGroup?) {
        repo.deleteBeaconGroup(group?.let { BeaconGroupEntity.from(it) })
    }

    override suspend fun delete(beacon: Beacon) {
        repo.deleteBeacon(BeaconEntity.from(beacon))
    }

}