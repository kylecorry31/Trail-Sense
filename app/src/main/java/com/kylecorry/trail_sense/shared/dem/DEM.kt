package com.kylecorry.trail_sense.shared.dem

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.util.Size
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.cache.GeospatialCache
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.luna.cache.LRUCache
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.SolMath.wrap
import com.kylecorry.sol.math.analysis.Trigonometry
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
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.data.LocalInputStreamable
import com.kylecorry.trail_sense.shared.data.SingleImageReader
import com.kylecorry.trail_sense.shared.extensions.ThreadParallelExecutor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object DEM {
    private const val CACHE_DISTANCE = 10f
    private const val CACHE_SIZE = 500
    private var cache = GeospatialCache<Float>(Distance.meters(CACHE_DISTANCE), size = CACHE_SIZE)
    private val multiElevationLookupLock = Mutex()
    private var gridCache = LRUCache<String, List<List<Pair<Coordinate, Float>>>>(1)
    private var cachedSources: List<GeographicImageSource>? = null
    private var cachedIsExternal: Boolean? = null
    private val sourcesLock = Mutex()

    suspend fun getElevation(location: Coordinate): Float = onDefault {
        cache.getOrPut(location) {
            lookupElevations(listOf(location))[location]
                ?: 0f
        }
    }

    private suspend fun getElevations(
        locations: List<Coordinate>,
        limitToBounds: CoordinateBounds
    ): Map<Coordinate, Float> =
        onDefault {
            // It is less performant to use the cache for large numbers of locations
            val shouldUseCache = locations.size < 20
            multiElevationLookupLock.withLock {
                val results = mutableMapOf<Coordinate, Float>()
                val cachedLocations = mutableSetOf<Coordinate>()

                if (shouldUseCache) {
                    val cached = cache.getAll(locations)
                    if (cached.isNotEmpty()) {
                        cachedLocations.addAll(cached.keys)
                        results.putAll(cached)
                    }
                }

                val remaining = if (shouldUseCache) {
                    locations.filter { it !in cachedLocations }
                } else {
                    locations
                }
                val elevations = lookupElevations(remaining, limitToBounds)
                if (shouldUseCache) {
                    cache.putAll(elevations)
                }

                results.putAll(elevations)

                if (remaining.isNotEmpty()) {
                    Log.d("DEM", "Looked up ${remaining.size} locations not in cache")
                }
                results
            }
        }

    private suspend fun getElevationGrid(
        bounds: CoordinateBounds,
        resolution: Double
    ): List<List<Pair<Coordinate, Float>>> = onDefault {
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

        gridCache.getOrPut(getGridKey(latitudes, longitudes, resolution)) {
            val toLookupCoordinates = ArrayList<Coordinate>(latitudes.size * longitudes.size)
            latitudes.forEach { lat ->
                longitudes.forEach { lon ->
                    toLookupCoordinates.add(Coordinate(lat, Coordinate.toLongitude(lon)))
                }
            }

            val allElevations = getElevations(toLookupCoordinates, bounds)
            var i = 0
            latitudes.map {
                longitudes.map {
                    val location = toLookupCoordinates[i++]
                    location to (allElevations[location] ?: 0f)
                }
            }
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
        val grid = getElevationGrid(bounds, resolution)

        var minElevation = Float.MAX_VALUE
        var maxElevation = Float.MIN_VALUE
        for (row in grid) {
            for (point in row) {
                if (point.second < minElevation) {
                    minElevation = point.second
                }
                if (point.second > maxElevation) {
                    maxElevation = point.second
                }
            }
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

    suspend fun elevationImage(
        bounds: CoordinateBounds,
        resolution: Double,
        colorMap: (elevation: Float, minElevation: Float, maxElevation: Float) -> Int = { elevation, minElevation, maxElevation ->
            val gray = SolMath.map(elevation, minElevation, maxElevation, 0f, 255f, true).toInt()
            Color.rgb(gray, gray, gray)
        }
    ): Bitmap = onDefault {
        val expandBy = 1
        val grid = getElevationGrid(bounds, resolution)
        val width = grid[0].size - expandBy * 2
        val height = grid.size - expandBy * 2
        val pixels = IntArray(height * width)

        try {
            var minElevation = Float.MAX_VALUE
            var maxElevation = Float.MIN_VALUE
            for (row in grid) {
                for (point in row) {
                    if (point.second < minElevation) {
                        minElevation = point.second
                    }
                    if (point.second > maxElevation) {
                        maxElevation = point.second
                    }
                }
            }

            for (y in expandBy..grid.lastIndex - expandBy) {
                for (x in expandBy..grid[y].lastIndex - expandBy) {
                    val color = colorMap(grid[y][x].second, minElevation, maxElevation)
                    pixels.set(x - expandBy, y - expandBy, width, color)
                }
            }
            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun hillshadeImage(
        bounds: CoordinateBounds,
        resolution: Double,
        zFactor: Float = 1f,
        azimuth: Float = 315f,
        altitude: Float = 45f,
        samples: Int = 1,
        sampleSpacing: Float = 3f
    ): Bitmap = onDefault {
        val expandBy = 1
        val grid = getElevationGrid(bounds, resolution)
        val width = grid[0].size - expandBy * 2
        val height = grid.size - expandBy * 2
        val pixels = IntArray(width * height)

        try {
            val getElevation = { x: Int, y: Int ->
                grid[y.coerceIn(grid.indices)][x.coerceIn(grid[0].indices)].second
            }

            val cellSizeX = (resolution * 111319.5 * cosDegrees(bounds.center.latitude))
            val cellSizeY = (resolution * 111319.5)

            // https://pro.arcgis.com/en/pro-app/latest/tool-reference/3d-analyst/how-hillshade-works.htm
            val zenithRad = (90 - altitude).toRadians()
            val azimuths = mutableListOf<Float>()
            var start = azimuth - (samples / 2) * sampleSpacing
            for (i in 0 until samples) {
                azimuths.add(Trigonometry.remapUnitAngle(start, 90f, true).toRadians())
                start += sampleSpacing
            }
            val cosZenith = cos(zenithRad)
            val sinZenith = sin(zenithRad)

            for (y in expandBy..grid.lastIndex - expandBy) {
                for (x in expandBy..grid[y].lastIndex - expandBy) {
                    val a = getElevation(x - 1, y - 1)
                    val b = getElevation(x, y - 1)
                    val c = getElevation(x + 1, y - 1)
                    val d = getElevation(x - 1, y)
                    val f = getElevation(x + 1, y)
                    val g = getElevation(x - 1, y + 1)
                    val h = getElevation(x, y + 1)
                    val i = getElevation(x + 1, y + 1)
                    val dx = (((c + 2 * f + i) - (a + 2 * d + g)) / (8 * cellSizeX)).toFloat()
                    val dy = (((g + 2 * h + i) - (a + 2 * b + c)) / (8 * cellSizeY)).toFloat()
                    val slopeRad = atan(zFactor * hypot(dx, dy))

                    var aspectRad = 0f
                    if (!SolMath.isZero(dx)) {
                        aspectRad = wrap(atan2(dy, -dx), 0f, 2 * PI.toFloat())
                    } else {
                        if (dy > 0) {
                            aspectRad = PI.toFloat() / 2
                        } else if (dy < 0) {
                            aspectRad = 3 * PI.toFloat() / 2
                        }
                    }

                    var hillshade = 0.0
                    for (azimuthRad in azimuths) {
                        hillshade += 255 * (cosZenith * cos(slopeRad) +
                                sinZenith * sin(slopeRad) * cos(azimuthRad - aspectRad)) / samples
                    }

                    val gray = hillshade.toInt().coerceIn(0, 255)
                    pixels.set(x - expandBy, y - expandBy, width, Color.rgb(gray, gray, gray))
                }
            }
            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565)
        } catch (e: Exception) {
            throw e
        }
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

    // TODO: If at the border of a tile, load the nearby pixels as well
    private suspend fun lookupElevations(
        locations: List<Coordinate>,
        limitToBounds: CoordinateBounds = CoordinateBounds.from(locations)
    ): Map<Coordinate, Float> =
        onIO {
            if (locations.isEmpty()) {
                return@onIO emptyMap()
            }

            val sources = getSources().filter { it.bounds.intersects(limitToBounds) }

            val lookups =
                locations.groupBy { location -> sources.firstOrNull { it.contains(location) } }

            val elevations = mutableMapOf<Coordinate, Float>()
            for (lookup in lookups) {
                if (lookup.key == null) {
                    lookup.value.forEach {
                        elevations[it] = 0f
                    }
                    continue
                }

                val coordinates =
                    lookup.value.associateBy { lookup.key!!.getPixel(it) }

                tryOrDefault(Distance.meters(0f)) {
                    // TODO: Load pixels without interpolation and interpolate later - or add a multi image lookup?
                    val readings = lookup.key!!.read(coordinates.keys.toList())

                    readings.forEach {
                        val coordinate = coordinates[it.first] ?: return@forEach
                        elevations[coordinate] = it.second.first()
                    }
                }
            }


            elevations
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