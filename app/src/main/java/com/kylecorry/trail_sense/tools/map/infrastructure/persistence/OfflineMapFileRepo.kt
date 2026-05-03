package com.kylecorry.trail_sense.tools.map.infrastructure.persistence

import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class OfflineMapFileRepo private constructor() {

    private val dao = getAppService<AppDatabase>().offlineMapFileDao()
    private val files = getAppService<FileSubsystem>()

    fun getAll(): Flow<List<OfflineMapFile>> = dao.getAll()
        .map { it.map { entity -> entity.toOfflineMapFile() } }
        .flowOn(Dispatchers.IO)

    suspend fun getAllSync(): List<OfflineMapFile> = onIO {
        dao.getAllSync().map { it.toOfflineMapFile() }
    }

    suspend fun add(file: OfflineMapFile): Long = onIO {
        dao.upsert(OfflineMapFileEntity.from(file))
    }

    suspend fun delete(file: OfflineMapFile) = onIO {
        dao.delete(OfflineMapFileEntity.from(file))
        files.delete(file.path)
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
