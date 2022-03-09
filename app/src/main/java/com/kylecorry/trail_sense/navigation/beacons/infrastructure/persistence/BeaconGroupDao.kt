package com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence

import androidx.room.*

@Dao
interface BeaconGroupDao {
    @Query("SELECT * FROM beacon_groups WHERE parent IS :parent")
    suspend fun getAllWithParent(parent: Long?): List<BeaconGroupEntity>

    @Query("SELECT * FROM beacon_groups WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): BeaconGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(beaconGroup: BeaconGroupEntity): Long

    @Delete
    suspend fun delete(beaconGroup: BeaconGroupEntity)

    @Update
    suspend fun update(beaconGroup: BeaconGroupEntity)
}