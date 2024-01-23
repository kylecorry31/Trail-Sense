package com.kylecorry.trail_sense.tools.packs.infrastructure

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PackItemDao {
    @Query("SELECT * FROM items")
    fun getAll(): LiveData<List<PackItemEntity>>

    @Query("SELECT * FROM items WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): PackItemEntity?

    @Query("SELECT * FROM items WHERE packId = :packId")
    suspend fun getFromPackAsync(packId: Long): List<PackItemEntity>

    @Query("SELECT * FROM items WHERE packId = :packId")
    fun getFromPack(packId: Long): LiveData<List<PackItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PackItemEntity): Long

    @Delete
    suspend fun delete(item: PackItemEntity)

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Query("DELETE FROM items WHERE packId = :packId")
    suspend fun deleteAllFromPack(packId: Long)

    @Update
    suspend fun update(item: PackItemEntity)

    @Query("UPDATE items SET amount = 0 WHERE packId = :packId")
    suspend fun clearPackedAmounts(packId: Long)
}