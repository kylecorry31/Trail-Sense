package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.CoordinateFormat

class MapRepo private constructor(private val context: Context) : IMapRepo {

    // TODO: Switch to a database
    private val maps = mutableListOf(
        Map(
            2, "Mount Washington", "maps/mount_washington.jpg",
            listOf(
                MapCalibrationPoint(
                    Coordinate.parse(
                        "19T 0315000E 4910000N",
                        CoordinateFormat.UTM
                    )!!, PercentCoordinate(0.09356582f, 0.14533052f)
                ),
                MapCalibrationPoint(
                    Coordinate.parse(
                        "19T 0321000E 4903000N",
                        CoordinateFormat.UTM
                    )!!, PercentCoordinate(0.8290716f, 0.7575625f)
                )
            )
        )
    )

    override fun getMaps(): LiveData<List<Map>> {
        return object : MutableLiveData<List<Map>>(null) {
            override fun onActive() {
                super.onActive()
                value = maps
            }
        }
    }

    override suspend fun getMap(id: Long): Map? {
        return maps.firstOrNull { it.id == id }
    }

    override suspend fun deleteMap(map: Map) {
        maps.removeIf { it.id == map.id }
    }

    override suspend fun addMap(map: Map): Long {
        return if (map.id == 0L) {
            val newId = maps.maxByOrNull { it.id }?.id ?: 1
            maps.add(map.copy(id = newId))
            newId
        } else {
            deleteMap(map)
            maps.add(map)
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