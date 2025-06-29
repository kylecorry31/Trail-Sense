package com.kylecorry.trail_sense.shared.dem

import android.util.Log
import android.util.Size
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.cache.GeospatialCache
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.ParallelCoroutineRunner
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.getConnectedLines
import com.kylecorry.trail_sense.shared.andromeda_temp.getIsolineCalculators
import com.kylecorry.trail_sense.shared.andromeda_temp.getMultiplesBetween
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DEM {
    private val cacheDistance = 10f
    private val cacheSize = 500
    private var cache = GeospatialCache<Distance>(Distance.meters(cacheDistance), size = cacheSize)
    private val multiElevationLookupLock = Mutex()

    suspend fun getElevation(location: Coordinate): Distance? = onDefault {
        cache.getOrPut(location) {
            lookupElevations(listOf(location)).first().second
        }
    }

    suspend fun getElevations(locations: List<Coordinate>): List<Pair<Coordinate, Distance>> =
        onDefault {
            multiElevationLookupLock.withLock {
                val results = mutableListOf<Pair<Coordinate, Distance>>()
                val cachedLocations = mutableSetOf<Coordinate>()

                for (location in locations) {
                    val cached = cache.get(location)
                    if (cached != null) {
                        cachedLocations.add(location)
                        results.add(location to cached)
                    }
                }

                val remaining = locations.filter { it !in cachedLocations }
                val elevations = lookupElevations(remaining)
                for (elevation in elevations) {
                    cache.put(elevation.first, elevation.second)
                    results.add(elevation)
                }

                if (remaining.isNotEmpty()) {
                    Log.d("DEM", "Looked up ${remaining.size} locations not in cache")
                }
                results
            }
        }

    /**
     * Get contour lines using marching squares
     */
    suspend fun getContourLines(
        bounds: CoordinateBounds,
        interval: Float,
        resolution: Double,
    ): List<Contour> = onDefault {
        val latitudes = Interpolation.getMultiplesBetween(
            bounds.south - resolution,
            bounds.north + resolution,
            resolution
        )

        val longitudes = Interpolation.getMultiplesBetween(
            bounds.west - resolution,
            bounds.east + resolution,
            resolution
        )

        val toLookup = latitudes.map { lat ->
            longitudes.map { lon -> Coordinate(lat, lon) }
        }

        val allElevations =
            getElevations(toLookup.flatten()).map { it.first to it.second.meters().distance }
        val grid = toLookup.map {
            it.map { coord ->
                val elevation = allElevations.find { it.first == coord }?.second ?: 0f
                coord to elevation
            }
        }

        val minElevation = grid.minOfOrNull { it.minOf { it.second } } ?: 0f
        val maxElevation = grid.maxOfOrNull { it.maxOf { it.second } } ?: 0f

        val thresholds = Interpolation.getMultiplesBetween(
            minElevation,
            maxElevation,
            interval
        )

        val parallelThresholds = ParallelCoroutineRunner(16)
        parallelThresholds.map(thresholds) { threshold ->
            val calculators = Interpolation.getIsolineCalculators<Coordinate>(
                grid,
                threshold,
                ::lerpCoordinate
            ) { a, b ->
                a.bearingTo(b).value
            }

            val parallel = ParallelCoroutineRunner(16)
            val segments = parallel.mapFunctions(calculators).flatten()

            Contour(
                threshold,
                Geometry.getConnectedLines(segments.map { it.start to it.end }),
                segments.map { lerpCoordinate(0.5f, it.start, it.end) to it.upDirection }
            )
        }
    }

    private fun lerpCoordinate(percent: Float, a: Coordinate, b: Coordinate): Coordinate {
        val distance = a.distanceTo(b)
        val bearing = a.bearingTo(b)
        return a.plus(distance * percent.toDouble(), bearing)
    }

    private suspend fun getSources(): List<Pair<String, GeographicImageSource>> = onIO {
        // Clean the repo before getting sources
        DEMRepo.getInstance().clean()
        var isExternal = isExternalModel()
        val tiles = if (isExternal) {
            val database = AppServiceRegistry.get<AppDatabase>().digitalElevationModelDao()
            database.getAll()
        } else {
            BuiltInDem.getTiles()
        }

        tiles.map {
            val valuePixelOffset = if (isExternal) {
                0.5f
            } else {
                // Built-in is heavily compressed, therefore this value was experimentally determined to have the best accuracy
                0.7f
            }
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
                valuePixelOffset = valuePixelOffset,
                interpolationOrder = 2
            )
        }
    }

    private suspend fun lookupElevations(locations: List<Coordinate>): List<Pair<Coordinate, Distance>> =
        onIO {
            val files = AppServiceRegistry.get<FileSubsystem>()
            val sources = getSources()
            val isExternal = isExternalModel()

            val lookups = locations.map { location ->
                location to sources.firstOrNull {
                    it.second.contains(location)
                }
            }.groupBy { it.second }

            val elevations = mutableListOf<Pair<Coordinate, Distance>>()
            for (lookup in lookups) {
                if (lookup.key == null) {
                    elevations.addAll(lookup.value.map { it.first to Distance.meters(0f) })
                    continue
                }

                val coordinates =
                    lookup.value.map { it.second!!.second.getPixel(it.first) to it.first }

                tryOrDefault(Distance.meters(0f)) {
                    val streamProvider = suspend {
                        if (isExternal) {
                            files.get(lookup.key!!.first).inputStream()
                        } else {
                            files.streamAsset(lookup.key!!.first)!!
                        }
                    }
                    val readings =
                        lookup.key!!.second.read(streamProvider, coordinates.map { it.first })
                    elevations.addAll(readings.mapNotNull {
                        val coordinate =
                            coordinates.firstOrNull { c -> c.first == it.first }?.second
                                ?: return@mapNotNull null
                        coordinate to Distance.meters(it.second.first())
                    })
                }
            }


            elevations
        }

    fun invalidateCache() {
        cache = GeospatialCache(Distance.meters(cacheDistance), size = cacheSize)
    }

    fun isExternalModel(): Boolean {
        val prefs = AppServiceRegistry.get<UserPreferences>()
        return prefs.altimeter.isDigitalElevationModelLoaded
    }

}