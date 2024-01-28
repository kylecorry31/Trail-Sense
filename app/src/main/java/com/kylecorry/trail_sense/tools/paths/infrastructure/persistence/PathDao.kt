package com.kylecorry.trail_sense.tools.paths.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PathDao {
    @Query("SELECT * FROM paths")
    fun getAll(): Flow<List<PathEntity>>

    @Query("SELECT * FROM paths")
    suspend fun getAllSuspend(): List<PathEntity>

    @Query("SELECT * FROM paths WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): PathEntity?

    @Query("SELECT * FROM paths where parentId IS :parentId")
    suspend fun getAllInGroup(parentId: Long?): List<PathEntity>

    @Query("SELECT * FROM paths WHERE _id = :id LIMIT 1")
    fun getLive(id: Long): LiveData<PathEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(path: PathEntity): Long

    @Delete
    suspend fun delete(path: PathEntity)

    @Query("DELETE FROM paths WHERE parentId is :parentId")
    suspend fun deleteInGroup(parentId: Long?)

    @Update
    suspend fun update(path: PathEntity)
}