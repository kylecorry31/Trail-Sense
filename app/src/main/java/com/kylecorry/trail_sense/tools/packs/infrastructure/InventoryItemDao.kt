package com.kylecorry.trail_sense.tools.packs.infrastructure

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface InventoryItemDao {
    @Query("SELECT * FROM items")
    fun getAll(): LiveData<List<InventoryItemEntity>>

    @Query("SELECT * FROM items WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): InventoryItemEntity?

    @Query("SELECT * FROM items WHERE packId = :packId")
    suspend fun getFromPackAsync(packId: Long): List<InventoryItemEntity>

    @Query("SELECT * FROM items WHERE packId = :packId")
    fun getFromPack(packId: Long): LiveData<List<InventoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItemEntity): Long

    @Delete
    suspend fun delete(item: InventoryItemEntity)

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Query("DELETE FROM items WHERE packId = :packId")
    suspend fun deleteAllFromPack(packId: Long)

    @Update
    suspend fun update(item: InventoryItemEntity)

    @Query("UPDATE items SET amount = 0 WHERE packId = :packId")
    suspend fun clearPackedAmounts(packId: Long)
}