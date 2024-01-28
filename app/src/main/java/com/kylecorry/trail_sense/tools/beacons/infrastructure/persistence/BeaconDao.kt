package com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BeaconDao {
    @Query("SELECT * FROM beacons WHERE `temporary` = 0")
    fun getAll(): Flow<List<BeaconEntity>>

    @Query("SELECT * FROM beacons WHERE `temporary` = 0")
    suspend fun getAllSuspend(): List<BeaconEntity>

    @Query("SELECT * FROM beacons WHERE `name` LIKE '%' || :text || '%' AND `temporary` = 0")
    suspend fun search(text: String): List<BeaconEntity>

    @Query("SELECT * FROM beacons WHERE beacon_group_id IS :groupId AND `name` LIKE '%' || :text || '%' AND `temporary` = 0")
    suspend fun searchInGroup(text: String, groupId: Long?): List<BeaconEntity>

    @Query("SELECT * FROM beacons where beacon_group_id IS :groupId AND `temporary` = 0")
    suspend fun getAllInGroup(groupId: Long?): List<BeaconEntity>

    @Query("SELECT * FROM beacons where `temporary` = 1 AND `owner` = :owner LIMIT 1")
    suspend fun getTemporaryBeacon(owner: Int): BeaconEntity?

    @Query("SELECT * FROM beacons WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): BeaconEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(beacon: BeaconEntity): Long

    @Delete
    suspend fun delete(beacon: BeaconEntity)

    @Query("DELETE FROM beacons WHERE beacon_group_id is :groupId")
    suspend fun deleteInGroup(groupId: Long?)

    @Update
    suspend fun update(beacon: BeaconEntity)

    /**
     * Get all beacons in a region. Does not work if the region crosses the 180 meridian, use getAllInRegionNear180Meridian instead.
     */
    @Query("SELECT * FROM beacons WHERE `temporary` = 0 AND latitude BETWEEN :south AND :north AND longitude BETWEEN :west AND :east")
    suspend fun getAllInRegion(
        north: Double,
        south: Double,
        east: Double,
        west: Double
    ): List<BeaconEntity>

    /**
     * Get all beacons in a region that crosses the 180 meridian
     */
    @Query("SELECT * FROM beacons WHERE `temporary` = 0 AND latitude BETWEEN :south AND :north AND (longitude >= :west OR longitude <= :east)")
    suspend fun getAllInRegionNear180Meridian(
        north: Double,
        south: Double,
        east: Double,
        west: Double
    ): List<BeaconEntity>
}