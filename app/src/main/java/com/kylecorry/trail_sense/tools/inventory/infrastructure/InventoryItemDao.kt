package com.kylecorry.trail_sense.tools.inventory.infrastructure

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kylecorry.trail_sense.tools.inventory.domain.InventoryItem

@Dao
interface InventoryItemDao {
    @Query("SELECT * FROM items")
    fun getAll(): LiveData<List<InventoryItem>>

    @Query("SELECT * FROM items WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItem): Long

    @Delete
    suspend fun delete(item: InventoryItem)

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Update
    suspend fun update(item: InventoryItem)
}