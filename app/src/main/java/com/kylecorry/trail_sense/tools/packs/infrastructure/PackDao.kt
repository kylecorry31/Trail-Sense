package com.kylecorry.trail_sense.tools.packs.infrastructure

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PackDao {
    @Query("SELECT * FROM packs")
    fun getAll(): LiveData<List<PackEntity>>

    @Query("SELECT * FROM packs")
    suspend fun getAllAsync(): List<PackEntity>

    @Query("SELECT * FROM packs WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): PackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PackEntity): Long

    @Delete
    suspend fun delete(item: PackEntity)

    @Update
    suspend fun update(item: PackEntity)
}