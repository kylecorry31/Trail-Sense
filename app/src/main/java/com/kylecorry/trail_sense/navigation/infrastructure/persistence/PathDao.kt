package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PathDao {
    @Query("SELECT * FROM paths")
    fun getAll(): LiveData<List<PathEntity>>

    @Query("SELECT * FROM paths")
    suspend fun getAllSuspend(): List<PathEntity>

    @Query("SELECT * FROM paths WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): PathEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(path: PathEntity): Long

    @Delete
    suspend fun delete(path: PathEntity)

    @Update
    suspend fun update(path: PathEntity)
}