package com.kylecorry.trail_sense.tools.paths.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PathGroupDao {

    @Query("SELECT * FROM path_groups WHERE parent IS :parent")
    suspend fun getAllWithParent(parent: Long?): List<PathGroupEntity>

    @Query("SELECT * FROM path_groups WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): PathGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: PathGroupEntity): Long

    @Delete
    suspend fun delete(group: PathGroupEntity)

    @Update
    suspend fun update(group: PathGroupEntity)
}