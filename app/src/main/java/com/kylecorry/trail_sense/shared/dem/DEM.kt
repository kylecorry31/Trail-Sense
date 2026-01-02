package com.kylecorry.trail_sense.shared.dem

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.cache.GeospatialCache
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.luna.cache.LRUCache
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.getMultiplesBetween2
import com.kylecorry.trail_sense.shared.andromeda_temp.set
import com.kylecorry.trail_sense.shared.data.AssetInputStreamable
import com.kylecorry.trail_sense.shared.data.EncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.FloatBitmap
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.data.LocalInputStreamable
import com.kylecorry.trail_sense.shared.data.SingleImageReader
import com.kylecorry.trail_sense.shared.extensions.ThreadParallelExecutor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DEM {

    private class ElevationBitmap(
        val data: FloatBitmap,
        val latitudes: DoubleArray,
        val longitudes: DoubleArray
    )

    private const val CACHE_DISTANCE = 10f
    private const val CACHE_SIZE = 500
    private var cache = GeospatialCache<Float>(Distance.meters(CACHE_DISTANCE), size = CACHE_SIZE)
    private var pixelCache = LRUCache<String, ElevationBitmap>(1)
    private var tileCache = LRUCache<String, ElevationBitmap>(50)
    private var cachedSources: List<GeographicImageSource>? = null
    private var cachedIsExternal: Boolean? = null
    private val sourcesLock = Mutex()

    suspend fun getElevation(location: Coordinate): Float = onDefault {
        cache.getOrPut(location) {
            val source = getSources().firstOrNull { it.contains(location) } ?: return@getOrPut 0f
            onIO {
                tryOrDefault(0f) {
                    source.read(location).first()
                }
            }
        }
    }

    private suspend fun getElevations(
        bounds: CoordinateBounds,
        resolution: Double,
        isTile: Boolean = false
    ): ElevationBitmap = onDefault {
        val latitudes = Interpolation.getMultiplesBetween2(
            bounds.south - resolution,
            bounds.north + resolution,
            resolution
        )

        val longitudes = Interpolation.getMultiplesBetween2(
            bounds.west - resolution,
            (if (bounds.west < bounds.east) bounds.east else bounds.east + 360) + resolution,
            resolution
        )

        val cache = if (isTile) {
            tileCache
        } else {
            pixelCache
        }

        cache.getOrPut(getGridKey(latitudes, longitudes, resolution)) {
            val width = longitudes.size
            val height = latitudes.size
            val output = FloatBitmap(width, height, 1)

            val sources = getSources().filter { it.bounds.intersects(bounds) }

            for (i in longitudes.indices) {
                longitudes[i] = Coordinate.toLongitude(longitudes[i])
            }

            for (i in sources.indices) {
                sources[i].read(latitudes, longitudes, output)
            }

            ElevationBitmap(output, latitudes, longitudes)
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
        val elevations = getElevations(bounds, resolution)

        var minElevation = Float.MAX_VALUE
        var maxElevation = Float.MIN_VALUE

        val grid = ArrayList<List<Pair<Coordinate, Float>>>(elevations.latitudes.size)
        for (y in elevations.latitudes.indices) {
            val row = ArrayList<Pair<Coordinate, Float>>(elevations.longitudes.size)
            val lat = elevations.latitudes[y]
            for (x in elevations.longitudes.indices) {
                val lon = Coordinate.toLongitude(elevations.longitudes[x])
                val value = elevations.data.get(x, y, 0)

                if (value < minElevation) {
                    minElevation = value
                }
                if (value > maxElevation) {
                    maxElevation = value
                }

                row.add(Coordinate(lat, lon) to value)
            }
            grid.add(row)
        }

        val thresholds = Interpolation.getMultiplesBetween(
            minElevation,
            maxElevation,
            interval
        )

        Parallel.map(thresholds) { threshold ->
            val segments = Interpolation.getIsoline(
                grid,
                threshold,
                executor = ThreadParallelExecutor(),
                interpolator = ::lerpCoordinate
            )

            Contour(
                threshold,
                Geometry.getConnectedLines(segments.map { it.start to it.end })
            )
        }
    }

    suspend fun getElevationImage(
        bounds: CoordinateBounds,
        resolution: Double,
        config: Bitmap.Config = Bitmap.Config.RGB_565,
        adjuster: (x: Int, y: Int, getElevation: (x: Int, y: Int) -> Float) -> Int
    ): Bitmap = onDefault {
        val expandBy = 1
        val elevations = getElevations(bounds, resolution, true)
        val width = elevations.data.width - expandBy * 2
        val height = elevations.data.height - expandBy * 2
        val pixels = IntArray(width * height)

        val getElevation = { x: Int, y: Int ->
            elevations.data.get(x, y, 0)
        }

        for (y in expandBy until elevations.data.height - expandBy) {
            for (x in expandBy until elevations.data.width - expandBy) {
                pixels.set(x - expandBy, y - expandBy, width, adjuster(x, y, getElevation))
            }
        }

        Bitmap.createBitmap(pixels, width, height, config)
    }

    private fun lerpCoordinate(percent: Float, a: Coordinate, b: Coordinate): Coordinate {
        val distance = a.distanceTo(b)
        val bearing = a.bearingTo(b)
        return a.plus(distance * percent.toDouble(), bearing)
    }

    private suspend fun getSources(): List<GeographicImageSource> = onIO {
        sourcesLock.withLock {
            // Clean the repo before getting sources
            DEMRepo.getInstance().clean()
            val isExternal = isExternalModel()
            val previousSources = cachedSources
            if (previousSources != null && cachedIsExternal == isExternal) {
                return@onIO previousSources
            }

            val tiles = if (isExternal) {
                val database = AppServiceRegistry.get<AppDatabase>().digitalElevationModelDao()
                database.getAll()
            } else {
                BuiltInDem.getTiles()
            }

            val sources = tiles.map {
                val valuePixelOffset = if (isExternal) {
                    0.5f
                } else {
                    // Built-in is heavily compressed, therefore this value was experimentally determined to have the best accuracy
                    0.7f
                }
                // TODO: Support tiles with different decoders or an aggregated geographic image source
                GeographicImageSource(
                    EncodedDataImageReader(
                        SingleImageReader(
                            Size(it.width, it.height), if (!isExternal) {
                                AssetInputStreamable(it.filename)
                            } else {
                                LocalInputStreamable(it.filename)
                            }
                        ),
                        decoder = if (it.compressionMethod == "8-bit") EncodedDataImageReader.scaledDecoder(
                            it.a,
                            it.b
                        ) else EncodedDataImageReader.split16BitDecoder(it.a, it.b),
                        treatZeroAsNaN = true,
                        maxChannels = 1
                    ),
                    bounds = CoordinateBounds(
                        it.north,
                        it.east,
                        it.south,
                        it.west
                    ),
                    precision = 10,
                    valuePixelOffset = valuePixelOffset,
                    interpolationOrder = 2,
                )
            }
            cachedSources = sources
            cachedIsExternal = isExternal
            sources
        }
    }

    fun invalidateCache() {
        cache = GeospatialCache(Distance.meters(CACHE_DISTANCE), size = CACHE_SIZE)
        cachedSources = null
        cachedIsExternal = null
    }

    fun isExternalModel(): Boolean {
        val prefs = AppServiceRegistry.get<UserPreferences>()
        return prefs.altimeter.isDigitalElevationModelLoaded
    }

    private fun getGridKey(
        latitudes: DoubleArray,
        longitudes: DoubleArray,
        resolution: Double
    ): String {
        val minLatitude = latitudes.firstOrNull() ?: 0.0
        val maxLatitude = latitudes.lastOrNull() ?: 0.0
        val minLongitude = longitudes.firstOrNull() ?: 0.0
        val maxLongitude = longitudes.lastOrNull() ?: 0.0
        return "${minLatitude}_${maxLatitude}_${minLongitude}_${maxLongitude}_$resolution"
    }

}