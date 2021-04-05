package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.kylecorry.trail_sense.shared.AppDatabase
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapEntity

class MapRepo private constructor(context: Context) : IMapRepo {

    private val mapDao = AppDatabase.getInstance(context).mapDao()

    override fun getMaps(): LiveData<List<Map>> {
        return Transformations.map(mapDao.getAll()){it.map { it.toMap() }}
    }

    override suspend fun getMap(id: Long): Map? {
        return mapDao.get(id)?.toMap()
    }

    override suspend fun deleteMap(map: Map) {
        mapDao.delete(MapEntity.from(map))
    }

    override suspend fun addMap(map: Map): Long {
        return if (map.id == 0L) {
            mapDao.insert(MapEntity.from(map))
        } else {
            mapDao.update(MapEntity.from(map))
            map.id
        }
    }

    companion object {
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