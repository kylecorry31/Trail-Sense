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
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.Vector3
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
import kotlin.math.pow

object DEM {
    private val cacheDistance = 10f
    private val cacheSize = 500
    private var cache = GeospatialCache2<Distance>(Distance.meters(cacheDistance), size = cacheSize)
    private val multiElevationLookupLock = Mutex()

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
                for (elevation in elevations) {
                    results.add(elevation)
                }

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

        val toLookup = latitudes.map { lat ->
            longitudes.map { lon -> Coordinate(lat, lon) }
        }

        val allElevations =
            getElevations(toLookup.flatten()).map { it.first to it.second.meters().distance }
        toLookup.map {
            it.map { coord ->
                val elevation = allElevations.find { it.first == coord }?.second ?: 0f
                coord to elevation
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
        resolution: Double
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
                val i = SolMath.map(getElevation(x, y), minElevation, maxElevation, 0f, 255f, true)
                    .toInt()
                val color = Color.rgb(i, i, i)
                bitmap[x, y] = Color.rgb(color, color, color)
            }
        }

        bitmap
    }

    suspend fun hillshadeImage(
        bounds: CoordinateBounds,
        resolution: Double,
        power: Double = 2.0
    ): Bitmap = onDefault {
        val grid = getElevationGrid(bounds, resolution)

        val bitmap = createBitmap(grid[0].size, grid.size, Bitmap.Config.RGB_565)

        val getElevation = { x: Int, y: Int ->
            grid[y.coerceIn(grid.indices)][x.coerceIn(grid[0].indices)].second
        }

        // Scale the deltas by the horizontal resolution
        val dzScale = (1f / (resolution * 111319.5 * cosDegrees(bounds.center.latitude))).toFloat()

        // 315 degrees azimuth,  45 altitude
        val light = Vector3(-0.5f, 0.5f, 0.70710677f)
        for (y in grid.indices) {
            for (x in grid[y].indices) {
                val x1 = getElevation(x - 1, y)
                val x2 = getElevation(x + 1, y)
                val y1 = getElevation(x, y - 1)
                val y2 = getElevation(x, y + 1)
                val n = Vector3((x1 - x2) * dzScale, (y1 - y2) * dzScale, 2f).normalize()
                val raw = light.dot(n).coerceAtLeast(0f)
                val gray = (raw.toDouble().pow(power) * 255).toInt()
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
                interpolationOrder = 2
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

}