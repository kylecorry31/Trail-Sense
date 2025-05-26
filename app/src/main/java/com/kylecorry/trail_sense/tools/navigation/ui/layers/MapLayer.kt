package com.kylecorry.trail_sense.tools.navigation.ui.layers

import android.graphics.Bitmap
import android.graphics.Rect
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sinh
import kotlin.math.tan

class MapLayer : ILayer {

    private var isInvalid = true
    private var maps: List<PhotoMap> = emptyList()
    private var lastBounds: CoordinateBounds? = null
    private var tiles: Map<CoordinateBounds, List<Bitmap>> = emptyMap()
    private val runner = CoroutineQueueRunner()
    private val scope = CoroutineScope(Dispatchers.Default)


    fun setMaps(maps: List<PhotoMap>) {
        this.maps = maps
        invalidate()
    }

    fun setBounds(bounds: CoordinateBounds?) {
        if (bounds == lastBounds) {
            return
        }
        lastBounds = bounds
        invalidate()
    }

    fun loadTiles(bounds: CoordinateBounds, widthPx: Int, heightPx: Int) {
        if (!isInvalid) return

        // Step 1: Split the visible area into tiles (geographic)
        val tiles = getTiles(bounds, widthPx, heightPx)

        // Step 2: Order maps by scale
        val sortedMaps = maps.filter { it.isCalibrated }.sortedBy {
            it.distancePerPixel()
        }

        // Step 3: For each tile, determine which map(s) will supply it. Algorithm: Always include the first map that contains a piece of the tile. If the full tile isn't contained, choose the next map which contains the highest percent of the tile.
        val tileSources = mutableMapOf<CoordinateBounds, List<PhotoMap>>()
        for (tile in tiles) {
            val contained =
                sortedMaps.firstOrNull {
                    contains(
                        it.boundary() ?: return@firstOrNull false,
                        tile,
                        fullyContained = true
                    )
                }
            if (contained != null) {
                tileSources[tile] = listOf(contained)
            }
        }

        // Step 4: (background) Load tiles for each image / unload irrelevant tiles
        scope.launch {
            runner.replace {
                val newTiles = mutableMapOf<CoordinateBounds, List<Bitmap>>()
                synchronized(this@MapLayer.tiles) {
                    this@MapLayer.tiles.keys.forEach { key ->
                        if (!tileSources.any { areEqual(key, it.key) }) {
                            this@MapLayer.tiles[key]?.forEach { bitmap -> bitmap.recycle() }
                        } else {
                            // If the tile is still relevant, keep it
                            newTiles[key] = this@MapLayer.tiles[key]!!
                        }
                    }

                    this@MapLayer.tiles = emptyMap()
                }
                for (source in tileSources) {
                    if (newTiles.any { areEqual(source.key, it.key) }) {
                        continue
                    }
                    // Load tiles from the bitmap
                    val entries = mutableListOf<Bitmap>()
                    source.value.forEach {
                        val fileSystem = AppServiceRegistry.get<FileSubsystem>()
                        val projection = it.projection

                        val west = projection.toPixels(source.key.northWest)
                            .x.toInt()
                        val east = projection.toPixels(source.key.southEast)
                            .x.toInt()
                        val north = projection.toPixels(source.key.northWest)
                            .y.toInt()
                        val south = projection.toPixels(source.key.southEast)
                            .y.toInt()

                        val region = Rect(
                            min(west, east),
                            min(north, south),
                            max(west, east),
                            max(north, south)
                        )
                        fileSystem.streamLocal(it.filename).use { stream ->
                            entries.add(BitmapUtils.decodeRegion(stream, region) ?: return@forEach)
                        }
                    }

                    newTiles[source.key] = entries
                }

                synchronized(this@MapLayer.tiles) {
                    this@MapLayer.tiles = newTiles
                }
            }
        }


        isInvalid = false
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        val bounds = getBounds(drawer)

        loadTiles(lastBounds ?: return, bounds.width().toInt(), bounds.height().toInt())

        // Step 5: Render loaded tiles
        synchronized(tiles) {
            tiles.forEach { (tileBounds, bitmaps) ->
                bitmaps.reversed().forEach { bitmap ->
                    val topLeftPixel = map.toPixel(tileBounds.northWest)
                    val bottomRightPixel = map.toPixel(tileBounds.southEast)
                    drawer.image(
                        bitmap,
                        min(topLeftPixel.x, bottomRightPixel.x),
                        min(topLeftPixel.y, bottomRightPixel.y),
                        abs(bottomRightPixel.x - topLeftPixel.x),
                        abs(bottomRightPixel.y - topLeftPixel.y)
                    )
                }
            }
        }
    }

    override fun drawOverlay(drawer: ICanvasDrawer, map: IMapView) {
        // Do nothing
    }

    override fun invalidate() {
        isInvalid = true
    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        return false
    }

    private fun getBounds(drawer: ICanvasDrawer): Rectangle {
        // Rotating by map rotation wasn't working around 90/270 degrees - this is a workaround
        // It will just render slightly more of the path than needed, but never less (since 45 is when the area is at its largest)
        return drawer.getBounds(45f)
    }

    private fun areEqual(bounds1: CoordinateBounds, bounds2: CoordinateBounds): Boolean {
        return SolMath.isZero((bounds1.north - bounds2.north).toFloat()) &&
                SolMath.isZero((bounds1.west - bounds2.west).toFloat()) &&
                SolMath.isZero((bounds1.south - bounds2.south).toFloat()) &&
                SolMath.isZero((bounds1.east - bounds2.east).toFloat())
    }

    private fun contains(
        bounds: CoordinateBounds,
        subBounds: CoordinateBounds,
        fullyContained: Boolean = false
    ): Boolean {
        // TODO: Project to mercator then do geometry check
        val corners = listOf(
            bounds.contains(subBounds.northWest),
            bounds.contains(subBounds.northEast),
            bounds.contains(subBounds.southWest),
            bounds.contains(subBounds.southEast),
            bounds.contains(subBounds.center)
        )

        return if (fullyContained) {
            corners.all { it }
        } else {
            corners.any { it }
        }
    }

    fun getTiles(
        bounds: CoordinateBounds,
        widthPx: Int,
        heightPx: Int
    ): List<CoordinateBounds> {
        val minLat = max(bounds.south, -85.0511)
        val maxLat = min(bounds.north, 85.0511)
        val zoom = boundsToZoom(minLat, bounds.west, maxLat, bounds.east, widthPx, heightPx)

        val (xMin, yMax) = latLonToTileXY(minLat, bounds.west, zoom)
        val (xMax, yMin) = latLonToTileXY(maxLat, bounds.east, zoom)

        val tiles = mutableListOf<CoordinateBounds>()
        for (x in min(xMin, xMax)..max(xMin, xMax)) {
            for (y in min(yMin, yMax)..max(yMin, yMax)) {
                tiles.add(tileXYToBounds(x, y, zoom))
            }
        }

        return tiles
    }

    private fun latLonToTileXY(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
        val latRad = Math.toRadians(lat)
        val n = 1 shl zoom
        val x = ((lon + 180.0) / 360.0 * n).toInt()
        val y = ((1.0 - ln(tan(latRad) + 1 / cos(latRad)) / PI) / 2.0 * n).toInt()
        return x to y
    }

    private fun tileXYToBounds(x: Int, y: Int, zoom: Int): CoordinateBounds {
        val n = 1 shl zoom
        val lonMin = x / n.toDouble() * 360.0 - 180.0
        val lonMax = (x + 1) / n.toDouble() * 360.0 - 180.0

        val latRadMin = atan(sinh(PI * (1 - 2 * (y + 1).toDouble() / n)))
        val latRadMax = atan(sinh(PI * (1 - 2 * y.toDouble() / n)))

        val latMin = Math.toDegrees(latRadMin)
        val latMax = Math.toDegrees(latRadMax)

        return CoordinateBounds(latMin, lonMax, latMax, lonMin)
    }

    private fun boundsToZoom(
        minLat: Double,
        minLon: Double,
        maxLat: Double,
        maxLon: Double,
        widthPx: Int,
        heightPx: Int
    ): Int {
        fun mercatorY(lat: Double): Double {
            val rad = Math.toRadians(lat)
            return ln(tan(PI / 4 + rad / 2))
        }

        val worldTileSize = 256.0
        val latFraction = (mercatorY(maxLat) - mercatorY(minLat)) / (2 * PI)
        val lonFraction = (maxLon - minLon) / 360.0

        val latZoom = log2(heightPx / worldTileSize / latFraction)
        val lonZoom = log2(widthPx / worldTileSize / lonFraction)

        return floor(min(latZoom, lonZoom)).toInt()
    }

}