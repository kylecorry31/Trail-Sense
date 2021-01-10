package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.weather.domain.PressureReadingEntity

@Dao
interface BeaconDao {
    @Query("SELECT * FROM beacons")
    fun getAll(): LiveData<List<BeaconEntity>>

    @Query("SELECT * FROM beacons")
    suspend fun getAllSuspend(): List<BeaconEntity>

    @Query("SELECT * FROM beacons where beacon_group_id IS :groupId")
    suspend fun getAllInGroup(groupId: Long?): List<BeaconEntity>

    @Query("SELECT * FROM beacons WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): BeaconEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(beacon: BeaconEntity): Long

    @Delete
    suspend fun delete(beacon: BeaconEntity)

    @Query("DELETE FROM beacons WHERE beacon_group_id = :groupId")
    suspend fun deleteInGroup(groupId: Long?)

    @Update
    suspend fun update(beacon: BeaconEntity)
}