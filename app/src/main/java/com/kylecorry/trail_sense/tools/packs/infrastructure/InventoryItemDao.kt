package com.kylecorry.trail_sense.tools.packs.infrastructure

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kylecorry.trail_sense.tools.packs.domain.InventoryItemDto

@Dao
interface InventoryItemDao {
    @Query("SELECT * FROM items")
    fun getAll(): LiveData<List<InventoryItemDto>>

    @Query("SELECT * FROM items WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): InventoryItemDto?

    @Query("SELECT * FROM items WHERE packId = :packId")
    suspend fun getFromPackAsync(packId: Long): List<InventoryItemDto>

    @Query("SELECT * FROM items WHERE packId = :packId")
    fun getFromPack(packId: Long): LiveData<List<InventoryItemDto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItemDto): Long

    @Delete
    suspend fun delete(item: InventoryItemDto)

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Query("DELETE FROM items WHERE packId = :packId")
    suspend fun deleteAllFromPack(packId: Long)

    @Update
    suspend fun update(item: InventoryItemDto)

    @Query("UPDATE items SET amount = 0 WHERE packId = :packId")
    suspend fun clearPackedAmounts(packId: Long)
}