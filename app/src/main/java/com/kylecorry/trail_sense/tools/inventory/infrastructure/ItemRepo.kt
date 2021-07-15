package com.kylecorry.trail_sense.tools.inventory.infrastructure

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kylecorry.trail_sense.shared.AppDatabase
import com.kylecorry.trail_sense.tools.inventory.domain.InventoryItemDto
import com.kylecorry.trail_sense.tools.inventory.domain.Pack

class ItemRepo private constructor(context: Context) : IItemRepo {

    private val inventoryItemDao = AppDatabase.getInstance(context).inventoryItemDao()
    private val packDao = AppDatabase.getInstance(context).packDao()
    private val mapper = InventoryItemMapper()

    override fun getItems() = inventoryItemDao.getAll()

    override suspend fun getItemsFromPackAsync(packId: Long) =
        inventoryItemDao.getFromPackAsync(packId)

    override fun getItemsFromPack(packId: Long) = inventoryItemDao.getFromPack(packId)

    override fun getPacks(): LiveData<List<Pack>> {
        return Transformations.map(packDao.getAll()) { it.map { mapper.mapToPack(it) } }
    }

    override suspend fun getPacksAsync(): List<Pack> =
        packDao.getAllAsync().map { mapper.mapToPack(it) }

    override suspend fun getPack(packId: Long): Pack? {
        val pack = packDao.get(packId) ?: return null
        return mapper.mapToPack(pack)
    }

    override suspend fun getItem(id: Long) = inventoryItemDao.get(id)

    override suspend fun deleteItem(item: InventoryItemDto) = inventoryItemDao.delete(item)

    override suspend fun deletePack(pack: Pack) {
        packDao.delete(mapper.mapToPackEntity(pack))
    }

    override suspend fun addPack(pack: Pack): Long {
        return if (pack.id == 0L) {
            packDao.insert(mapper.mapToPackEntity(pack))
        } else {
            packDao.update(mapper.mapToPackEntity(pack))
            pack.id
        }
    }

    override suspend fun deleteAll() = inventoryItemDao.deleteAll()

    override suspend fun addItem(item: InventoryItemDto) {
        if (item.id != 0L) {
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