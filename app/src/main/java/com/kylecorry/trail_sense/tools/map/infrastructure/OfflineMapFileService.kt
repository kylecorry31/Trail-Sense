package com.kylecorry.trail_sense.tools.map.infrastructure

import com.kylecorry.trail_sense.shared.grouping.count.GroupCounter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupDeleter
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupLoader
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.map.domain.IOfflineMapFile
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFileGroup
import com.kylecorry.trail_sense.tools.map.infrastructure.persistence.OfflineMapFileRepo

class OfflineMapFileService {

    private val repo = getAppService<OfflineMapFileRepo>()

    val loader = GroupLoader(this::getGroup, this::getChildren)
    private val counter = GroupCounter(loader)

    private val deleter = object : GroupDeleter<IOfflineMapFile>(loader) {
        override suspend fun deleteItems(items: List<IOfflineMapFile>) {
            items.filterIsInstance<OfflineMapFile>().forEach { repo.delete(it) }
        }

        override suspend fun deleteGroup(group: IOfflineMapFile) {
            repo.deleteGroup(group as OfflineMapFileGroup)
        }
    }

    suspend fun add(file: IOfflineMapFile): Long {
        return if (file.isGroup) {
            repo.addGroup(file as OfflineMapFileGroup)
        } else {
            repo.add(file as OfflineMapFile)
        }
    }

    suspend fun delete(file: IOfflineMapFile) {
        deleter.delete(file)
    }

    suspend fun getGroup(id: Long?): OfflineMapFileGroup? {
        id ?: return null
        return repo.getGroup(id)?.copy(count = counter.count(id))
    }

    private suspend fun getGroups(parent: Long?): List<OfflineMapFileGroup> {
        return repo.getGroupsWithParent(parent).map { it.copy(count = counter.count(it.id)) }
    }

    private suspend fun getChildren(parentId: Long?): List<IOfflineMapFile> {
        val files = repo.getItemsWithParent(parentId)
        val groups = getGroups(parentId)
        return files + groups
    }

}
