package com.kylecorry.trail_sense.tools.maps.infrastructure

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.tools.inventory.domain.InventoryItem
import com.kylecorry.trail_sense.tools.maps.domain.Map

interface IMapRepo {
    fun getMaps(): LiveData<List<Map>>

    suspend fun getMap(id: Long): Map?

    suspend fun deleteMap(map: Map)

    suspend fun addMap(map: Map): Long
}