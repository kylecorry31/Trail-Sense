package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.MapEntity
import com.kylecorry.trail_sense.tools.maps.domain.MapGroup
import com.kylecorry.trail_sense.tools.maps.domain.MapGroupEntity
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap

class MapRepo private constructor(context: Context) : IMapRepo {

    private val mapDao = AppDatabase.getInstance(context).mapDao()
    private val mapGroupDao = AppDatabase.getInstance(context).mapGroupDao()
    private val files = FileSubsystem.getInstance(context)

    override suspend fun getAllMaps(): List<PhotoMap> = onIO {
        mapDao.getAll().map { convertToMap(it) }
    }

    override suspend fun getMapGroup(id: Long): MapGroup? = onIO {
        mapGroupDao.get(id)?.toMapGroup()
    }

    override suspend fun getMap(id: Long): PhotoMap? = onIO {
        mapDao.get(id)?.let { convertToMap(it) }
    }

    override suspend fun deleteMap(map: PhotoMap) = onIO {
        tryOrNothing { files.delete(map.filename) }
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
        mapDao.getAllWithParent(parentId).map { convertToMap(it) }
    }

    override suspend fun getMapGroups(parentId: Long?): List<MapGroup> = onIO {
        mapGroupDao.getAllWithParent(parentId).map { it.toMapGroup() }
    }

    private fun convertToMap(map: MapEntity): PhotoMap {
        val newMap = map.toMap()
        val size = files.imageSize(newMap.filename)
        val fileSize = files.size(newMap.filename)
        return newMap.copy(
            metadata = newMap.metadata.copy(
                size = Size(size.width.toFloat(), size.height.toFloat()),
                fileSize = fileSize
            )
        )
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: MapRepo? = null

        @Synchronized
        fun getInstance(context: Context): MapRepo {
            if (instance == null) {
                instance = MapRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}