package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.grouping.count.GroupCounter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupDeleter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo

class OfflineMapFileService {

    private val repo = getAppService<MapRepo>()

    val loader = GroupLoader(this::getGroup, this::getChildren)
    private val counter = GroupCounter(loader)

    private val deleter = object : GroupDeleter<IMap>(loader) {
        override suspend fun deleteItems(items: List<IMap>) {
            items.filterIsInstance<VectorMap>().forEach { repo.delete(it) }
        }

        override suspend fun deleteGroup(group: IMap) {
            repo.delete(group as MapGroup)
        }
    }

    suspend fun add(file: IMap): Long {
        return if (file.isGroup) {
            repo.add(file as MapGroup)
        } else {
            repo.add(file as VectorMap)
        }
    }

    suspend fun delete(file: IMap) {
        deleter.delete(file)
    }

    suspend fun getGroup(id: Long?): MapGroup? {
        id ?: return null
        return repo.getMapGroup(id)?.copy(count = counter.count(id))
    }

    private suspend fun getGroups(parent: Long?): List<MapGroup> {
        return repo.getMapGroups(parent).map { it.copy(count = counter.count(it.id)) }
    }

    private suspend fun getChildren(parentId: Long?): List<IMap> {
        val files = repo.getVectorMaps(parentId)
        val groups = getGroups(parentId)
        return files + groups
    }

}
