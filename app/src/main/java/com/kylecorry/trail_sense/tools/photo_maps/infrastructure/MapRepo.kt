package com.kylecorry.trail_sense.tools.photo_maps.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.math.MathUtils
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.ParallelCoroutineRunner
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapEntity
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapGroup
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapGroupEntity
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

class MapRepo private constructor(private val context: Context) : IMapRepo {

    private val mapDao = AppDatabase.getInstance(context).mapDao()
    private val mapGroupDao = AppDatabase.getInstance(context).mapGroupDao()
    private val files = FileSubsystem.getInstance(context)

    override suspend fun getAllMaps(): List<PhotoMap> = onIO {
        val maps = mapDao.getAll()
        val runner = ParallelCoroutineRunner(MAX_PARALLEL)
        runner.map(maps, ::convertToMap)
    }

    override suspend fun getMapGroup(id: Long): MapGroup? = onIO {
        mapGroupDao.get(id)?.toMapGroup()
    }

    override suspend fun getMap(id: Long): PhotoMap? = onIO {
        mapDao.get(id)?.let { convertToMap(it) }
    }

    override suspend fun deleteMap(map: PhotoMap) = onIO {
        tryOrNothing { files.delete(map.filename) }
        tryOrNothing { files.delete(map.pdfFileName) }
        mapDao.delete(MapEntity.from(map))
    }

    override suspend fun deleteMapGroup(group: MapGroup) {
        mapGroupDao.delete(MapGroupEntity.from(group))
    }

    override suspend fun addMapGroup(group: MapGroup): Long = onIO {
        if (group.id != 0L) {
            mapGroupDao.update(MapGroupEntity.from(group))
            group.id
        } else {
            mapGroupDao.insert(MapGroupEntity.from(group))
        }
    }

    override suspend fun addMap(map: PhotoMap): Long = onIO {
        if (map.id == 0L) {
            mapDao.insert(MapEntity.from(map))
        } else {
            mapDao.update(MapEntity.from(map))
            map.id
        }
    }

    override suspend fun getMaps(parentId: Long?): List<PhotoMap> = onIO {
        val maps = mapDao.getAllWithParent(parentId)
        val runner = ParallelCoroutineRunner(MAX_PARALLEL)
        runner.map(maps, ::convertToMap)
    }

    override suspend fun getMapGroups(parentId: Long?): List<MapGroup> = onIO {
        mapGroupDao.getAllWithParent(parentId).map { it.toMapGroup() }
    }

    private fun convertToMap(map: MapEntity): PhotoMap {
        val newMap = map.toMap()
        // TODO: Save the size in the DB
        val size = files.imageSize(newMap.filename)
        val fileSize = files.size(newMap.filename) + files.size(newMap.pdfFileName)

        val pdfSize =
            if (map.pdfHeight != null && map.pdfWidth != null && files.get(newMap.pdfFileName)
                    .exists()
            ) {

                val scaledSize = MathUtils.scaleToBounds(
                    android.util.Size(map.pdfWidth, map.pdfHeight),
                    android.util.Size(PhotoMap.DESIRED_PDF_SIZE, PhotoMap.DESIRED_PDF_SIZE)
                )

                Size(scaledSize.width.toFloat(), scaledSize.height.toFloat())
            } else {
                null
            }

        return newMap.copy(
            metadata = newMap.metadata.copy(
                size = pdfSize ?: Size(size.width.toFloat(), size.height.toFloat()),
                fileSize = fileSize
            )
        )
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: MapRepo? = null

        private val MAX_PARALLEL = 10

        @Synchronized
        fun getInstance(context: Context): MapRepo {
            if (instance == null) {
                instance = MapRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}