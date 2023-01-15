package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapEntity
import com.kylecorry.trail_sense.tools.maps.domain.MapGroup
import com.kylecorry.trail_sense.tools.maps.domain.MapGroupEntity

class MapRepo private constructor(context: Context) : IMapRepo {

    private val mapDao = AppDatabase.getInstance(context).mapDao()
    private val mapGroupDao = AppDatabase.getInstance(context).mapGroupDao()
    private val files = FileSubsystem.getInstance(context)

    override fun getMapsLive(): LiveData<List<Map>> {
        return Transformations.map(mapDao.getAll()) { it.map(this::convertToMap) }
    }

    override suspend fun getMapGroup(id: Long): MapGroup? = onIO {
        mapGroupDao.get(id)?.toMapGroup()
    }

    override suspend fun getMap(id: Long): Map? = onIO {
        mapDao.get(id)?.let { convertToMap(it) }
    }

    override suspend fun deleteMap(map: Map) = onIO {
        files.delete(map.filename)
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

    override suspend fun addMap(map: Map): Long = onIO {
        if (map.id == 0L) {
            mapDao.insert(MapEntity.from(map))
        } else {
            mapDao.update(MapEntity.from(map))
            map.id
        }
    }

    override suspend fun getMaps(parentId: Long?): List<Map> = onIO {
        mapDao.getAllWithParent(parentId).map { convertToMap(it) }
    }

    override suspend fun getMapGroups(parentId: Long?): List<MapGroup> = onIO {
        mapGroupDao.getAllWithParent(parentId).map { it.toMapGroup() }
    }

    private fun convertToMap(map: MapEntity): Map {
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