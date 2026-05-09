package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence

import android.annotation.SuppressLint
import android.content.Context
import android.util.Size
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.math.MathUtils
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.luna.coroutines.ParallelCoroutineRunner
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance.PersistentTileCache
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapEntity
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups.MapGroupEntity
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.persistence.VectorMapEntity
import com.kylecorry.trail_sense.tools.offline_maps.map_layers.PhotoMapTileSource

class MapRepo private constructor(context: Context) {
    private val offlineMapFileDao = AppDatabase.getInstance(context).vectorMapDao()
    private val photoMapDao = AppDatabase.getInstance(context).photoMapDao()
    private val mapGroupDao = AppDatabase.getInstance(context).mapGroupDao()
    private val files = FileSubsystem.getInstance(context)
    private val tileCache = getAppService<PersistentTileCache>()

    suspend fun getPhotoMaps(): List<PhotoMap> = onIO {
        val photoMaps = photoMapDao.getAll()
        val runner = ParallelCoroutineRunner(MAX_PARALLEL)
        runner.map(photoMaps, ::convertToMap)
    }

    suspend fun getVectorMaps(): List<VectorMap> = onIO {
        offlineMapFileDao.getAllSync().map { it.toOfflineMapFile() }
    }

    suspend fun getMapGroup(id: Long): MapGroup? = onIO {
        mapGroupDao.get(id)?.toMapGroup()
    }

    suspend fun getPhotoMap(id: Long): PhotoMap? = onIO {
        photoMapDao.get(id)?.let { convertToMap(it) }
    }

    suspend fun getVectorMap(id: Long): VectorMap? = onIO {
        offlineMapFileDao.get(id)?.toOfflineMapFile()
    }

    suspend fun delete(map: PhotoMap) = onIO {
        tryOrNothing { files.delete(map.filename) }
        tryOrNothing { files.delete(map.pdfFileName) }
        photoMapDao.delete(PhotoMapEntity.from(map))
        invalidatePhotoMapCache(map.id)
    }

    suspend fun delete(map: VectorMap) = onIO {
        tryOrNothing { files.delete(map.path) }
        offlineMapFileDao.delete(VectorMapEntity.from(map))
    }

    suspend fun delete(group: MapGroup) {
        mapGroupDao.delete(MapGroupEntity.from(group))
    }

    suspend fun add(group: MapGroup): Long = onIO {
        if (group.id != 0L) {
            mapGroupDao.update(MapGroupEntity.from(group))
            group.id
        } else {
            mapGroupDao.insert(MapGroupEntity.from(group))
        }
    }

    suspend fun add(map: PhotoMap): Long = onIO {
        val newId = if (map.id == 0L) {
            photoMapDao.insert(PhotoMapEntity.from(map))
        } else {
            photoMapDao.update(PhotoMapEntity.from(map))
            map.id
        }
        invalidatePhotoMapCache(newId)
        newId
    }

    suspend fun add(file: VectorMap): Long = onIO {
        offlineMapFileDao.upsert(VectorMapEntity.from(file))
    }

    suspend fun getPhotoMaps(parentId: Long?): List<PhotoMap> = onIO {
        val maps = photoMapDao.getAllWithParent(parentId)
        val runner = ParallelCoroutineRunner(MAX_PARALLEL)
        runner.map(maps, ::convertToMap)
    }

    suspend fun getVectorMaps(parentId: Long?): List<VectorMap> = onIO {
        offlineMapFileDao.getAllWithParent(parentId).map { it.toOfflineMapFile() }
    }

    suspend fun getMapGroups(parentId: Long?): List<MapGroup> = onIO {
        mapGroupDao.getAllWithParent(parentId).map { it.toMapGroup() }
    }

    private suspend fun invalidatePhotoMapCache(mapId: Long) {
        val cacheKeys = listOf(
            "${PhotoMapTileSource.SOURCE_ID}-true-$mapId",
            "${PhotoMapTileSource.SOURCE_ID}-false-$mapId",
            "${PhotoMapTileSource.SOURCE_ID}-null-$mapId",
            "${PhotoMapTileSource.SOURCE_ID}-true",
            "${PhotoMapTileSource.SOURCE_ID}-false",
            "${PhotoMapTileSource.SOURCE_ID}-null",
        )
        cacheKeys.forEach {
            tileCache.invalidate(it)
        }
    }

    private fun convertToMap(map: PhotoMapEntity): PhotoMap {
        val newMap = map.toMap()
        // TODO: Save the size in the DB
        val size = files.imageSize(newMap.filename)
        val fileSize = files.size(newMap.filename) + files.size(newMap.pdfFileName)

        val pdfSize =
            if (map.pdfHeight != null && map.pdfWidth != null && files.get(newMap.pdfFileName)
                    .exists()
            ) {

                val scaledSize = MathUtils.scaleToBounds(
                    Size(map.pdfWidth, map.pdfHeight),
                    Size(PhotoMap.DESIRED_PDF_SIZE, PhotoMap.DESIRED_PDF_SIZE)
                )

                com.kylecorry.sol.math.geometry.Size(
                    scaledSize.width.toFloat(),
                    scaledSize.height.toFloat()
                )
            } else {
                null
            }

        return newMap.copy(
            metadata = newMap.metadata.copy(
                size = pdfSize ?: com.kylecorry.sol.math.geometry.Size(
                    size.width.toFloat(),
                    size.height.toFloat()
                ),
                fileSize = fileSize,
                imageSize = com.kylecorry.sol.math.geometry.Size(
                    size.width.toFloat(),
                    size.height.toFloat()
                )
            )
        )
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: MapRepo? = null

        private const val MAX_PARALLEL = 10

        @Synchronized
        fun getInstance(context: Context): MapRepo {
            if (instance == null) {
                instance = MapRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}
