package com.kylecorry.trail_sense.tools.packs.infrastructure

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.tools.packs.domain.InventoryItemDto
import com.kylecorry.trail_sense.tools.packs.domain.Pack

interface IItemRepo {
    fun getItems(): LiveData<List<InventoryItemDto>>

    suspend fun getItem(id: Long): InventoryItemDto?

    suspend fun getItemsFromPackAsync(packId: Long): List<InventoryItemDto>

    fun getItemsFromPack(packId: Long): LiveData<List<InventoryItemDto>>

    fun getPacks(): LiveData<List<Pack>>

    suspend fun getPacksAsync(): List<Pack>

    suspend fun getPack(packId: Long): Pack?

    suspend fun deleteItem(item: InventoryItemDto)

    suspend fun deletePack(pack: Pack)

    suspend fun addPack(pack: Pack): Long

    suspend fun addItem(item: InventoryItemDto)

    suspend fun deleteAll()

    suspend fun clearPackedAmounts(packId: Long)

    suspend fun copyPack(fromPack: Pack, toPack: Pack): Long
}