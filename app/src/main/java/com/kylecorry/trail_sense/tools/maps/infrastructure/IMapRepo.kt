package com.kylecorry.trail_sense.tools.maps.infrastructure

import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapGroup

interface IMapRepo {
    suspend fun getAllMapFiles(): List<String>

    suspend fun getMapGroup(id: Long): MapGroup?
    suspend fun getMap(id: Long): Map?

    suspend fun deleteMap(map: Map)
    suspend fun deleteMapGroup(group: MapGroup)

    suspend fun addMapGroup(group: MapGroup): Long
    suspend fun addMap(map: Map): Long

    suspend fun getMaps(parentId: Long?): List<Map>
    suspend fun getMapGroups(parentId: Long?): List<MapGroup>
}