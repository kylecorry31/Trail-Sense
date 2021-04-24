package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.domain.BeaconGroupEntity
import com.kylecorry.trail_sense.shared.AppDatabase
import com.kylecorry.trailsensecore.domain.navigation.BeaconOwner

class BeaconRepo private constructor(context: Context) : IBeaconRepo {

    private val beaconDao = AppDatabase.getInstance(context).beaconDao()
    private val beaconGroupDao = AppDatabase.getInstance(context).beaconGroupDao()

    override fun getBeacons(): LiveData<List<BeaconEntity>> = beaconDao.getAll()

    override suspend fun getBeaconsSync(): List<BeaconEntity> = beaconDao.getAllSuspend()

    override suspend fun searchBeacons(text: String): List<BeaconEntity> = beaconDao.search(text)

    override suspend fun searchBeaconsInGroup(text: String, groupId: Long?): List<BeaconEntity> = beaconDao.searchInGroup(text, groupId)

    override suspend fun getBeaconsInGroup(groupId: Long?): List<BeaconEntity> =
        beaconDao.getAllInGroup(groupId)

    override suspend fun getBeacon(id: Long): BeaconEntity? = beaconDao.get(id)

    override suspend fun getTemporaryBeacon(owner: BeaconOwner): BeaconEntity? = beaconDao.getTemporaryBeacon(owner.id)

    override suspend fun deleteBeacon(beacon: BeaconEntity) = beaconDao.delete(beacon)

    override suspend fun addBeacon(beacon: BeaconEntity): Long {
        return if (beacon.id != 0L) {
            beaconDao.update(beacon)
            beacon.id
        } else {
            beaconDao.insert(beacon)
        }
    }

    override suspend fun addBeaconGroup(group: BeaconGroupEntity): Long {
        return if (group.id != 0L) {
            beaconGroupDao.update(group)
            group.id
        } else {
            beaconGroupDao.insert(group)
        }
    }

    override suspend fun deleteBeaconGroup(group: BeaconGroupEntity) {
        beaconDao.deleteInGroup(group.id)
        beaconGroupDao.delete(group)
    }

    override fun getGroups(): LiveData<List<BeaconGroupEntity>> = beaconGroupDao.getAll()

    override suspend fun getGroupsSync(): List<BeaconGroupEntity> = beaconGroupDao.getAllSuspend()

    override suspend fun getGroup(id: Long): BeaconGroupEntity? = beaconGroupDao.get(id)


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