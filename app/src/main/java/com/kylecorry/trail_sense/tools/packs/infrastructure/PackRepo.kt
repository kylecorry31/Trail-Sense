package com.kylecorry.trail_sense.tools.packs.infrastructure

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem

class PackRepo private constructor(context: Context) : IPackRepo {

    private val inventoryItemDao = AppDatabase.getInstance(context).packItemDao()
    private val packDao = AppDatabase.getInstance(context).packDao()
    private val mapper = PackMapper()

    override suspend fun getItemsFromPackAsync(packId: Long) =
        inventoryItemDao.getFromPackAsync(packId).map { mapper.mapToPackItem(it) }

    override fun getItemsFromPack(packId: Long): LiveData<List<PackItem>> {
        return inventoryItemDao.getFromPack(packId).map {
            it.map { item -> mapper.mapToPackItem(item) }
        }
    }

    override fun getPacks(): LiveData<List<Pack>> {
        return packDao.getAll().map { it.map { mapper.mapToPack(it) } }
    }

    override suspend fun getPacksAsync(): List<Pack> =
        packDao.getAllAsync().map { mapper.mapToPack(it) }

    override suspend fun getPack(packId: Long): Pack? {
        val pack = packDao.get(packId) ?: return null
        return mapper.mapToPack(pack)
    }

    override suspend fun getItem(id: Long): PackItem? {
        val item = inventoryItemDao.get(id) ?: return null
        return mapper.mapToPackItem(item)
    }

    override suspend fun deleteItem(item: PackItem) = inventoryItemDao.delete(mapper.mapToItemEntity(item))

    override suspend fun deletePack(pack: Pack) {
        inventoryItemDao.deleteAllFromPack(pack.id)
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

    override suspend fun clearPackedAmounts(packId: Long) =
        inventoryItemDao.clearPackedAmounts(packId)

    override suspend fun copyPack(fromPack: Pack, toPack: Pack): Long {
        val newId = addPack(toPack)
        val items = getItemsFromPackAsync(fromPack.id)
        val toItems = items.map { it.copy(id = 0, packId = newId) }
        toItems.forEach {
            addItem(it)
        }
        return newId
    }

    override suspend fun addItem(item: PackItem) {
        if (item.id != 0L) {
            inventoryItemDao.update(mapper.mapToItemEntity(item))
        } else {
            inventoryItemDao.insert(mapper.mapToItemEntity(item))
        }
    }

    companion object {
        private var instance: PackRepo? = null

        @Synchronized
        fun getInstance(context: Context): PackRepo {
            if (instance == null) {
                instance = PackRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}