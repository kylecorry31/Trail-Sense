package com.kylecorry.trail_sense.shared.dem

import android.util.Size
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.cache.GeospatialCache
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.ParallelCoroutineRunner
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.io.FileSubsystem

object DEM {
    private var cache = GeospatialCache<Distance>(Distance.meters(10f), size = 200)

    suspend fun getElevation(location: Coordinate): Distance? = onDefault {
        cache.getOrPut(location) {
            lookupElevation(location)
        }
    }

    suspend fun getElevations(locations: List<Coordinate>): List<Pair<Coordinate, Distance>> =
        onDefault {
            val parallel = ParallelCoroutineRunner()
            parallel.map(locations) {
                val cached = cache.get(it)
                if (cached != null) {
                    it to cached
                } else {
                    val result = lookupElevation(it)
                    cache.put(it, result)
                    it to result
                }
            }
        }

    private suspend fun lookupElevation(location: Coordinate): Distance = onIO {
        val files = AppServiceRegistry.get<FileSubsystem>()
        val isExternal = isExternalModel()
        val tiles = if (isExternal) {
            val database = AppServiceRegistry.get<AppDatabase>().digitalElevationModelDao()
            database.getAll()
        } else {
            BuiltInDem.getTiles()
        }

        if (tiles.isEmpty()) {
            return@onIO Distance.meters(0f)
        }
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
                ?: return@onIO Distance.meters(0f)
        tryOrDefault(Distance.meters(0f)) {
            val stream = if (isExternal) {
                files.get(image.first).inputStream()
            } else {
                files.streamAsset(image.first)!!
            }
            stream.use {
                Distance.meters(image.second.read(it, location).first())
            }
        }
    }

    fun invalidateCache() {
        cache = GeospatialCache(Distance.meters(100f), size = 40)
    }

    fun isExternalModel(): Boolean {
        val prefs = AppServiceRegistry.get<UserPreferences>()
        return prefs.altimeter.isDigitalElevationModelLoaded
    }

}