package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kylecorry.trail_sense.navigation.domain.BeaconGroupEntity

@Dao
interface BeaconGroupDao {
    @Query("SELECT * FROM beacon_groups")
    fun getAll(): LiveData<List<BeaconGroupEntity>>

    @Query("SELECT * FROM beacon_groups")
    suspend fun getAllSuspend(): List<BeaconGroupEntity>

    @Query("SELECT * FROM beacon_groups WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): BeaconGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(beaconGroup: BeaconGroupEntity): Long

    @Delete
    suspend fun delete(beaconGroup: BeaconGroupEntity)

    @Update
    suspend fun update(beaconGroup: BeaconGroupEntity)
}