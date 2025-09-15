package com.kylecorry.trail_sense.shared.dem

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.util.Size
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.cache.GeospatialCache
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.luna.cache.LRUCache
import com.kylecorry.luna.coroutines.ParallelCoroutineRunner
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
import com.kylecorry.trail_sense.shared.data.AssetInputStreamable
import com.kylecorry.trail_sense.shared.data.EncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.data.LocalInputStreamable
import com.kylecorry.trail_sense.shared.data.SingleImageReader
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object DEM {
    private val cacheDistance = 10f
    private val cacheSize = 500
    private var cache = GeospatialCache<Float>(Distance.meters(cacheDistance), size = cacheSize)
    private val multiElevationLookupLock = Mutex()
    private var gridCache = LRUCache<String, List<List<Pair<Coordinate, Float>>>>(1)

    suspend fun getElevation(location: Coordinate): Float? = onDefault {
        cache.getOrPut(location) {
            lookupElevations(listOf(location)).first().second
        }
    }

    suspend fun getElevations(locations: List<Coordinate>): List<Pair<Coordinate, Float>> =
        onDefault {
            // It is less performant to use the cache for large numbers of locations
            val shouldUseCache = locations.size < 20
            multiElevationLookupLock.withLock {
                val results = mutableListOf<Pair<Coordinate, Float>>()
                val cachedLocations = mutableSetOf<Coordinate>()

                if (shouldUseCache) {
                    val cached = cache.getAll(locations)
                    if (cached.isNotEmpty()) {
                        cachedLocations.addAll(cached.keys)
                        results.addAll(cached.map { it.key to it.value })
                    }
                }

                val remaining = if (shouldUseCache) {
                    locations.filter { it !in cachedLocations }
                } else {
                    locations
                }
                val elevations = lookupElevations(remaining)
                if (shouldUseCache) {
                    cache.putAll(elevations.associate { it.first to it.second })
                }

                results.addAll(elevations)

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

        gridCache.getOrPut(getGridKey(latitudes, longitudes, resolution)) {
            val toLookupCoordinates = mutableListOf<Coordinate>()
            latitudes.forEach { lat ->
                longitudes.forEach { lon ->
                    toLookupCoordinates.add(Coordinate(lat, lon))
                }
            }

            val allElevations =
                getElevations(toLookupCoordinates).associate { it.first to it.second }
            var i = 0
            latitudes.map { lat ->
                longitudes.map { lon ->
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

        val minElevation = grid.minOfOrNull { it.minOf { it.second } } ?: 0f
        val maxElevation = grid.maxOfOrNull { it.maxOf { it.second } } ?: 0f

        val thresholds = Interpolation.getMultiplesBetween(
            minElevation,
            maxElevation,
            interval
        )

        val parallelThresholds = ParallelCoroutineRunner(16)
        parallelThresholds.map(thresholds) { threshold ->
            val calculators = Interpolation.getIsolineCalculators(
                grid,
                threshold,
                ::lerpCoordinate
            )

            val parallel = ParallelCoroutineRunner(16)
            val segments = parallel.mapFunctions(calculators).flatten()

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
        val grid = getElevationGrid(bounds, resolution)
        val bitmap = createBitmap(grid[0].size, grid.size, Bitmap.Config.RGB_565)

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

            for (y in grid.indices) {
                for (x in grid[y].indices) {
                    val color = colorMap(grid[y][x].second, minElevation, maxElevation)
                    bitmap[x, y] = color
                }
            }
        } catch (e: Exception) {
            bitmap.recycle()
            throw e
        }

        bitmap
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
        val grid = getElevationGrid(bounds, resolution)

        val bitmap = createBitmap(grid[0].size, grid.size, Bitmap.Config.RGB_565)

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

            for (y in grid.indices) {
                for (x in grid[y].indices) {
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
                    bitmap[x, y] = Color.rgb(gray, gray, gray)
                }
            }
        } catch (e: Exception) {
            bitmap.recycle()
            throw e
        }

        bitmap
    }

    private fun lerpCoordinate(percent: Float, a: Coordinate, b: Coordinate): Coordinate {
        val distance = a.distanceTo(b)
        val bearing = a.bearingTo(b)
        return a.plus(distance * percent.toDouble(), bearing)
    }

    private suspend fun getSources(): List<GeographicImageSource> = onIO {
        // TODO: Cache this
        // Clean the repo before getting sources
        DEMRepo.getInstance().clean()
        val isExternal = isExternalModel()
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
    }

    // TODO: If at the border of a tile, load the nearby pixels as well
    private suspend fun lookupElevations(locations: List<Coordinate>): List<Pair<Coordinate, Float>> =
        onIO {
            if (locations.isEmpty()) {
                return@onIO emptyList()
            }

            val sources = getSources()

            val lookups = locations.map { location ->
                location to sources.firstOrNull {
                    it.contains(location)
                }
            }.groupBy { it.second }

            val elevations = mutableListOf<Pair<Coordinate, Float>>()
            for (lookup in lookups) {
                if (lookup.key == null) {
                    elevations.addAll(lookup.value.map { it.first to 0f })
                    continue
                }

                val coordinates =
                    lookup.value.associate { it.second!!.getPixel(it.first) to it.first }

                tryOrDefault(Distance.meters(0f)) {
                    // TODO: Load pixels without interpolation and interpolate later - or add a multi image lookup?
                    val readings = lookup.key!!.read(coordinates.keys.toList())

                    elevations.addAll(readings.mapNotNull {
                        val coordinate = coordinates[it.first] ?: return@mapNotNull null
                        coordinate to it.second.first()
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

    private fun getGridKey(
        latitudes: List<Double>,
        longitudes: List<Double>,
        resolution: Double
    ): String {
        val minLatitude = latitudes.minOrNull() ?: 0.0
        val maxLatitude = latitudes.maxOrNull() ?: 0.0
        val minLongitude = longitudes.minOrNull() ?: 0.0
        val maxLongitude = longitudes.maxOrNull() ?: 0.0
        return "${minLatitude}_${maxLatitude}_${minLongitude}_${maxLongitude}_$resolution"
    }

}