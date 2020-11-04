package com.kylecorry.trail_sense.tools.inventory.infrastructure

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.tools.inventory.domain.InventoryItem

interface IItemRepo {
    fun getItems(): LiveData<List<InventoryItem>>

    suspend fun getItem(id: Long): InventoryItem?

    suspend fun deleteItem(item: InventoryItem)

    suspend fun addItem(item: InventoryItem)
}