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

class MapRepo private constructor(context: Context) : IMapRepo {

    private val mapDao = AppDatabase.getInstance(context).mapDao()
    private val files = FileSubsystem.getInstance(context)

    override fun getMapsLive(): LiveData<List<Map>> {
        return Transformations.map(mapDao.getAll()) { it.map(this::convertToMap) }
    }

    override suspend fun getMap(id: Long): Map? = onIO {
        mapDao.get(id)?.let { convertToMap(it) }
    }

    override suspend fun deleteMap(map: Map) = onIO {
        files.delete(map.filename)
        mapDao.delete(MapEntity.from(map))
    }

    override suspend fun addMap(map: Map): Long = onIO {
        if (map.id == 0L) {
            mapDao.insert(MapEntity.from(map))
        } else {
            mapDao.update(MapEntity.from(map))
            map.id
        }
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