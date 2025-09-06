package com.kylecorry.trail_sense.shared.dem

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.util.Size
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.luna.cache.LRUCache
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
import com.kylecorry.trail_sense.shared.ParallelCoroutineRunner
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.GeospatialCache2
import com.kylecorry.trail_sense.shared.andromeda_temp.getConnectedLines
import com.kylecorry.trail_sense.shared.andromeda_temp.getIsolineCalculators
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.io.FileSubsystem
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
    private var cache = GeospatialCache2<Distance>(Distance.meters(cacheDistance), size = cacheSize)
    private val multiElevationLookupLock = Mutex()
    private var gridCache = LRUCache<String, List<List<Pair<Coordinate, Float>>>>(1)

    suspend fun getElevation(location: Coordinate): Distance? = onDefault {
        cache.getOrPut(location) {
            lookupElevations(listOf(location)).first().second
        }
    }

    suspend fun getElevations(locations: List<Coordinate>): List<Pair<Coordinate, Distance>> =
        onDefault {
            // It is less performant to use the cache for large numbers of locations
            val shouldUseCache = locations.size < 20
            multiElevationLookupLock.withLock {
                val results = mutableListOf<Pair<Coordinate, Distance>>()
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
                getElevations(toLookupCoordinates).associate { it.first to it.second.meters().distance }
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

        val minElevation = grid.minOfOrNull { it.minOf { it.second } } ?: 0f
        val maxElevation = grid.maxOfOrNull { it.maxOf { it.second } } ?: 0f
        val getElevation = { x: Int, y: Int ->
            grid[y.coerceIn(grid.indices)][x.coerceIn(grid[0].indices)].second
        }

        for (y in grid.indices) {
            for (x in grid[y].indices) {
                val color = colorMap(getElevation(x, y), minElevation, maxElevation)
                bitmap[x, y] = color
            }
        }

        bitmap
    }

    suspend fun hillshadeImage(
        bounds: CoordinateBounds,
        resolution: Double,
        zFactor: Float = 1f,
        azimuth: Float = 315f,
        altitude: Float = 45f,
        samples: Int = 5,
        sampleSpacing: Float = 3f
    ): Bitmap = onDefault {
        val grid = getElevationGrid(bounds, resolution)

        val bitmap = createBitmap(grid[0].size, grid.size, Bitmap.Config.RGB_565)

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
                val color = Color.rgb(gray, gray, gray)
                bitmap[x, y] = Color.rgb(color, color, color)
            }
        }

        bitmap
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
                interpolationOrder = 2,
                maxChannels = 1
            )
        }
    }

    // TODO: If at the border of a tile, load the nearby pixels as well
    private suspend fun lookupElevations(locations: List<Coordinate>): List<Pair<Coordinate, Distance>> =
        onIO {
            if (locations.isEmpty()) {
                return@onIO emptyList()
            }

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
                    // TODO: Load pixels without interpolation and interpolate later - or add a multi image lookup?
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
        cache = GeospatialCache2(Distance.meters(cacheDistance), size = cacheSize)
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