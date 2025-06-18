package com.kylecorry.trail_sense.shared.dem

import android.util.Size
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.cache.GeospatialCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.io.FileSubsystem

object DEM {
    private var cache = GeospatialCache<Distance>(Distance.meters(10f), size = 40)

    suspend fun getElevation(location: Coordinate): Distance? = onIO {
        val files = AppServiceRegistry.get<FileSubsystem>()
        val database = AppServiceRegistry.get<AppDatabase>().digitalElevationModelDao()
        val tiles = database.getAll()

        if (tiles.isEmpty()) {
            return@onIO null
        }

        cache.getOrPut(location) {
            val sources = tiles.map {
                it.filename to GeographicImageSource(
                    Size(it.width, it.height),
                    bounds = CoordinateBounds(
                        it.north,
                        it.east,
                        it.south,
                        it.west
                    ),
                    decoder = if (it.compressionMethod == "8-bit") GeographicImageSource.scaledDecoder(
                        it.a,
                        it.b
                    ) else GeographicImageSource.split16BitDecoder(it.a, it.b),
                    precision = 10,
                    include0ValuesInInterpolation = false,
                    interpolationOrder = 2
                )
            }

            val image =
                sources.firstOrNull { it.second.contains(location) }
                    ?: return@getOrPut Distance.meters(0f)
            tryOrDefault(Distance.meters(0f)) {
                files.get(image.first).inputStream().use {
                    Distance.meters(image.second.read(it, location).first())
                }
            }
        }
    }

    fun invalidateCache() {
        cache = GeospatialCache(Distance.meters(100f), size = 40)
    }

    fun isAvailable(): Boolean {
        val prefs = AppServiceRegistry.get<UserPreferences>()
        return prefs.altimeter.isDigitalElevationModelLoaded
    }

}