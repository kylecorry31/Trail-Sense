package com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence

import androidx.room.*

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