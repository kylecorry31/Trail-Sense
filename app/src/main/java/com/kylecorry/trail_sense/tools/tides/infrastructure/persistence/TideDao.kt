package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TideDao {

    @Query("SELECT * FROM tides")
    fun getAll(): LiveData<List<TideEntity>>

    @Query("SELECT * FROM tides")
    suspend fun getAllSuspend(): List<TideEntity>

    @Query("SELECT * FROM tides WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): TideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tide: TideEntity): Long

    @Delete
    suspend fun delete(tide: TideEntity)

    @Query("DELETE FROM tides")
    suspend fun deleteAll()

    @Update
    suspend fun update(tide: TideEntity)

}