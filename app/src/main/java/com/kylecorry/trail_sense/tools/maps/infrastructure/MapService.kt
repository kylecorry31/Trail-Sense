package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.trail_sense.shared.grouping.count.GroupCounter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupDeleter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupLoader
import com.kylecorry.trail_sense.tools.maps.domain.IMap
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapGroup
import com.kylecorry.trail_sense.tools.maps.domain.MapProjectionType

class MapService private constructor(private val repo: IMapRepo) {

    val loader = GroupLoader(this::getGroup, this::getChildren)
    private val counter = GroupCounter(loader)

    private val deleter = object : GroupDeleter<IMap>(loader) {
        override suspend fun deleteItems(items: List<IMap>) {
            // TODO: Bulk delete
            items.forEach { repo.deleteMap(it as Map) }
        }

        override suspend fun deleteGroup(group: IMap) {
            repo.deleteMapGroup(group as MapGroup)
        }
    }

    suspend fun add(map: IMap): Long {
        return if (map.isGroup) {
            repo.addMapGroup(map as MapGroup)
        } else {
            repo.addMap(map as Map)
        }
    }

    suspend fun delete(map: IMap) {
        deleter.delete(map)
    }

    suspend fun setProjection(map: Map, projection: MapProjectionType): Map {
        val newMap = map.copy(metadata = map.metadata.copy(projection = projection))
        repo.addMap(newMap)
        return newMap
    }

    private suspend fun getGroups(parent: Long?): List<MapGroup> {
        return repo.getMapGroups(parent).map { it.copy(count = counter.count(it.id)) }
    }

    private suspend fun getChildren(parentId: Long?): List<IMap> {
        val paths = repo.getMaps(parentId)
        val groups = getGroups(parentId)
        return paths + groups
    }

    suspend fun getGroup(id: Long?): MapGroup? {
        id ?: return null
        return repo.getMapGroup(id)?.copy(count = counter.count(id))
    }

    suspend fun getMapFilenames(): List<String> {
        return repo.getAllMapFiles()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: MapService? = null

        @Synchronized
        fun getInstance(context: Context): MapService {
            if (instance == null) {
                instance = MapService(MapRepo.getInstance(context))
            }
            return instance!!
        }
    }

}