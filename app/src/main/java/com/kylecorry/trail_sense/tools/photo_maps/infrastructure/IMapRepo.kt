package com.kylecorry.trail_sense.tools.photo_maps.infrastructure

import com.kylecorry.trail_sense.tools.photo_maps.domain.MapGroup
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

interface IMapRepo {
    suspend fun getAllMaps(): List<PhotoMap>

    suspend fun getMapGroup(id: Long): MapGroup?
    suspend fun getMap(id: Long): PhotoMap?

    suspend fun deleteMap(map: PhotoMap)
    suspend fun deleteMapGroup(group: MapGroup)

    suspend fun addMapGroup(group: MapGroup): Long
    suspend fun addMap(map: PhotoMap): Long

    suspend fun getMaps(parentId: Long?): List<PhotoMap>
    suspend fun getMapGroups(parentId: Long?): List<MapGroup>
}