package com.kylecorry.trail_sense.tools.inventory.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.AppDatabase
import com.kylecorry.trail_sense.tools.inventory.domain.InventoryItem

class ItemRepo private constructor(context: Context) : IItemRepo {

    private val inventoryItemDao = AppDatabase.getInstance(context).inventoryItemDao()

    override fun getItems() = inventoryItemDao.getAll()

    override suspend fun getItem(id: Long) = inventoryItemDao.get(id)

    override suspend fun deleteItem(item: InventoryItem) = inventoryItemDao.delete(item)

    override suspend fun deleteAll() = inventoryItemDao.deleteAll()

    override suspend fun addItem(item: InventoryItem) {
        if (item.id != 0L){
            inventoryItemDao.update(item)
        } else {
            inventoryItemDao.insert(item)
        }
    }

    companion object {
        private var instance: ItemRepo? = null

        @Synchronized
        fun getInstance(context: Context): ItemRepo {
            if (instance == null) {
                instance = ItemRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}