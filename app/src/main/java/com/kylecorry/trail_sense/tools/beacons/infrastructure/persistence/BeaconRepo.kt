package com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence

import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.tools.beacons.BeaconsToolRegistration
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class BeaconRepo private constructor(context: Context) : IBeaconRepo {

    private val beaconDao = AppDatabase.getInstance(context).beaconDao()
    private val beaconGroupDao = AppDatabase.getInstance(context).beaconGroupDao()

    override fun getBeacons(): Flow<List<Beacon>> = beaconDao.getAll()
        .map { it.map { it.toBeacon() } }
        .flowOn(Dispatchers.IO)

    override suspend fun getBeaconsSync(): List<BeaconEntity> = beaconDao.getAllSuspend()

    override suspend fun searchBeacons(text: String): List<BeaconEntity> = beaconDao.search(text)

    override suspend fun searchBeaconsInGroup(text: String, groupId: Long?): List<BeaconEntity> =
        beaconDao.searchInGroup(text, groupId)

    override suspend fun getBeaconsInGroup(groupId: Long?): List<BeaconEntity> =
        beaconDao.getAllInGroup(groupId)

    override suspend fun getBeacon(id: Long): BeaconEntity? = beaconDao.get(id)

    override suspend fun getTemporaryBeacon(owner: BeaconOwner): BeaconEntity? =
        beaconDao.getTemporaryBeacon(owner.id)

    override suspend fun deleteBeacon(beacon: BeaconEntity) = onIO {
        beaconDao.delete(beacon)
        Tools.broadcast(BeaconsToolRegistration.BROADCAST_BEACONS_CHANGED)
    }

    override suspend fun addBeacon(beacon: BeaconEntity): Long = onIO {
        val newId = if (beacon.id != 0L) {
            beaconDao.update(beacon)
            beacon.id
        } else {
            beaconDao.insert(beacon)
        }
        Tools.broadcast(BeaconsToolRegistration.BROADCAST_BEACONS_CHANGED)
        newId
    }

    override suspend fun addBeaconGroup(group: BeaconGroupEntity): Long {
        return if (group.id != 0L) {
            beaconGroupDao.update(group)
            group.id
        } else {
            beaconGroupDao.insert(group)
        }
    }

    override suspend fun deleteBeaconGroup(group: BeaconGroupEntity?) {
        // Delete beacons
        beaconDao.deleteInGroup(group?.id)
        Tools.broadcast(BeaconsToolRegistration.BROADCAST_BEACONS_CHANGED)

        // Delete groups
        val groups = getGroupsWithParent(group?.id)
        for (subGroup in groups) {
            deleteBeaconGroup(subGroup)
        }

        // Delete self
        if (group != null) {
            beaconGroupDao.delete(group)
        }
    }

    override suspend fun getGroupsWithParent(parent: Long?): List<BeaconGroupEntity> =
        beaconGroupDao.getAllWithParent(parent)

    override suspend fun getGroup(id: Long): BeaconGroupEntity? = beaconGroupDao.get(id)

    override suspend fun getBeaconsInRegion(region: CoordinateBounds): List<BeaconEntity> {
        return if (region.east < region.west) {
            beaconDao.getAllInRegionNear180Meridian(
                region.north,
                region.south,
                region.east,
                region.west
            )
        } else {
            beaconDao.getAllInRegion(region.north, region.south, region.east, region.west)
        }
    }

    companion object {
        private var instance: BeaconRepo? = null

        @Synchronized
        fun getInstance(context: Context): BeaconRepo {
            if (instance == null) {
                instance = BeaconRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}