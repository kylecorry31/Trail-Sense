package com.kylecorry.trail_sense.tools.maps.infrastructure

import androidx.lifecycle.LiveData
import com.kylecorry.trailsensecore.domain.geo.cartography.Map

interface IMapRepo {
    fun getMaps(): LiveData<List<Map>>

    suspend fun getMap(id: Long): Map?

    suspend fun deleteMap(map: Map)

    suspend fun addMap(map: Map): Long
}