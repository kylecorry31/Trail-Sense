package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.persistence

import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.collections.map

class OfflineMapFileRepo private constructor() {

    private val dao = getAppService<AppDatabase>().offlineMapFileDao()
    private val groupDao = getAppService<AppDatabase>().offlineMapFileGroupDao()
    private val files = getAppService<FileSubsystem>()

    fun getAll(): Flow<List<OfflineMapFile>> = dao.getAll()
        .map { it.map { entity -> entity.toOfflineMapFile() } }
        .flowOn(Dispatchers.IO)

    suspend fun getAllSync(): List<OfflineMapFile> = onIO {
        dao.getAllSync().map { it.toOfflineMapFile() }
    }

    suspend fun get(id: Long): OfflineMapFile? = onIO {
        dao.get(id)?.toOfflineMapFile()
    }

    suspend fun getGroup(id: Long): MapGroup? = onIO {
        groupDao.get(id)?.toMapGroup()
    }

    suspend fun getGroupsWithParent(parent: Long?): List<MapGroup> = onIO {
        groupDao.getAllWithParent(parent).map { it.toMapGroup() }
    }

    suspend fun getItemsWithParent(parent: Long?): List<OfflineMapFile> = onIO {
        dao.getAllWithParent(parent).map { it.toOfflineMapFile() }
    }

    suspend fun add(file: OfflineMapFile): Long = onIO {
        dao.upsert(OfflineMapFileEntity.from(file))
    }

    suspend fun addGroup(group: MapGroup): Long = onIO {
        groupDao.upsert(OfflineMapFileGroupEntity.from(group))
    }

    suspend fun delete(file: OfflineMapFile) = onIO {
        dao.delete(OfflineMapFileEntity.from(file))
        files.delete(file.path)
    }

    suspend fun deleteGroup(group: MapGroup) = onIO {
        groupDao.delete(OfflineMapFileGroupEntity.from(group))
    }

    companion object {
        private var instance: OfflineMapFileRepo? = null

        @Synchronized
        fun getInstance(): OfflineMapFileRepo {
            if (instance == null) {
                instance = OfflineMapFileRepo()
            }
            return instance!!
        }
    }
}
