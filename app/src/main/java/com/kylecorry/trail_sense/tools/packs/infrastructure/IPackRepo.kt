package com.kylecorry.trail_sense.tools.packs.infrastructure

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem

interface IPackRepo {
    suspend fun getItem(id: Long): PackItem?

    suspend fun getItemsFromPackAsync(packId: Long): List<PackItem>

    fun getItemsFromPack(packId: Long): LiveData<List<PackItem>>

    fun getPacks(): LiveData<List<Pack>>

    suspend fun getPacksAsync(): List<Pack>

    suspend fun getPack(packId: Long): Pack?

    suspend fun deleteItem(item: PackItem)

    suspend fun deletePack(pack: Pack)

    suspend fun addPack(pack: Pack): Long

    suspend fun addItem(item: PackItem)

    suspend fun deleteAll()

    suspend fun clearPackedAmounts(packId: Long)

    suspend fun copyPack(fromPack: Pack, toPack: Pack): Long
}