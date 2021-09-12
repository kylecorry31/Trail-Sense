package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kylecorry.trail_sense.tools.tides.domain.TideEntity

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

    @Update
    suspend fun update(tide: TideEntity)

}