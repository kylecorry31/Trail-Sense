package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileLoader
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import kotlinx.coroutines.CancellationException
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

abstract class TileMapLayer<T : TileSource>(
    protected val source: T,
    private val taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask(),
    private var minZoomLevel: Int? = null
) : IAsyncLayer {

    private var shouldReloadTiles = true
    private var backgroundColor: Int = Color.WHITE
    protected val loader = TileLoader()
    protected val tilePaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    private val expandedBitmapPaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
    }
    var alpha: Int = 255
    private var updateListener: (() -> Unit)? = null
    private var zoomOffset: Int = 0
    private var cachedExpandedBitmap: Bitmap? = null
    private val cachedBitmapLock = Any()
    private val renderMatrix = Matrix()
    private val neighborMatrix = Matrix()
    private val srcPoints = FloatArray(8)
    private val dstPoints = FloatArray(8)

    fun setZoomOffset(offset: Int) {
        zoomOffset = offset
        shouldReloadTiles = true
    }

    open fun setBackgroundColor(color: Int) {
        this.backgroundColor = color
        shouldReloadTiles = true
    }

    fun setMinZoomLevel(level: Int) {
        minZoomLevel = level
        shouldReloadTiles = true
    }

    init {
        // Load tiles if needed
        taskRunner.addTask { viewBounds: Rectangle, bounds: CoordinateBounds, projection: IMapViewProjection ->
            shouldReloadTiles = false
            try {
                val tiles = getTiles(bounds, projection)

                if (tiles.size <= MAX_TILES &&
                    (tiles.firstOrNull()?.z ?: 0) >= (minZoomLevel ?: 0)
                ) {
                    loader.loadTiles(source, sortTiles(tiles))
                } else if (tiles.size > MAX_TILES) {
                    Log.d("TileLoader", "Too many tiles to load: ${tiles.size}")
                }

                updateListener?.invoke()
            } catch (e: CancellationException) {
                System.gc()
                throw e
            } catch (e: Throwable) {
                e.printStackTrace()
                shouldReloadTiles = true
            }
        }
    }

    private fun sortTiles(tiles: List<Tile>): List<Tile> {
        if (tiles.isEmpty()) return tiles
        val middleX = tiles.map { it.x }.average()
        val middleY = tiles.map { it.y }.average()
        return tiles.sortedBy { hypot(it.x - middleX, it.y - middleY) }
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        // Avoid drawing while in safe mode
        if (SafeMode.isEnabled()) {
            return
        }

        // Load tiles if needed
        taskRunner.scheduleUpdate(
            drawer.getBounds(45f), // TODO: Cache this
            map.mapBounds,
            map.mapProjection,
            shouldReloadTiles
        )

        // Render loaded tiles
        synchronized(loader.lock) {
            tilePaint.alpha = alpha
            renderTiles(drawer.canvas, map)
        }
    }

    override fun drawOverlay(drawer: ICanvasDrawer, map: IMapView) {
        // Do nothing
    }

    override fun invalidate() {
        // Do nothing, invalidation is handled separately
    }

    protected fun notifyListeners() {
        updateListener?.invoke()
    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        return false
    }

    private fun renderTiles(canvas: Canvas, map: IMapView) {
        loader.tileCache.entries.sortedBy { it.key.z }.forEach { (tile, bitmap) ->
            val tileBounds = tile.getBounds()
            synchronized(cachedBitmapLock) {
                val expandedBitmap = createExpandedTileBitmap(tile, bitmap)
                renderTile(
                    canvas,
                    map,
                    tileBounds,
                    expandedBitmap ?: bitmap,
                    expandedBitmap != null
                )
            }
        }
    }

    private fun renderTile(
        canvas: Canvas,
        map: IMapView,
        bounds: CoordinateBounds,
        bitmap: Bitmap,
        hasExpandedBorder: Boolean = false
    ) {
        val topLeftPixel = map.toPixel(bounds.northWest)
        val topRightPixel = map.toPixel(bounds.northEast)
        val bottomRightPixel = map.toPixel(bounds.southEast)
        val bottomLeftPixel = map.toPixel(bounds.southWest)

        val borderPixels = if (hasExpandedBorder) TILE_BORDER_PIXELS else 0

        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        // Bitmap pixels, exclude the border pixels
        // Top left
        srcPoints[0] = borderPixels.toFloat()
        srcPoints[1] = borderPixels.toFloat()
        // Top right
        srcPoints[2] = (bitmap.width - borderPixels).toFloat()
        srcPoints[3] = borderPixels.toFloat()
        // Bottom left
        srcPoints[4] = borderPixels.toFloat()
        srcPoints[5] = (bitmap.height - borderPixels).toFloat()
        // Bottom right
        srcPoints[6] = (bitmap.width - borderPixels).toFloat()
        srcPoints[7] = (bitmap.height - borderPixels).toFloat()

        // Canvas pixels
        // Top left
        dstPoints[0] = round(topLeftPixel.x)
        dstPoints[1] = round(topLeftPixel.y)
        // Top right
        dstPoints[2] = round(topRightPixel.x)
        dstPoints[3] = round(topRightPixel.y)
        // Bottom left
        dstPoints[4] = round(bottomLeftPixel.x)
        dstPoints[5] = round(bottomLeftPixel.y)
        // Bottom right
        dstPoints[6] = round(bottomRightPixel.x)
        dstPoints[7] = round(bottomRightPixel.y)

        renderMatrix.reset()
        renderMatrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        shader.setLocalMatrix(renderMatrix)
        tilePaint.shader = shader

        val minX = min(dstPoints[0], dstPoints[4])
        val maxX = max(dstPoints[2], dstPoints[6])
        val minY = min(dstPoints[1], dstPoints[3])
        val maxY = max(dstPoints[5], dstPoints[7])

        canvas.drawRect(minX, minY, maxX, maxY, tilePaint)

        tilePaint.shader = null
    }

    private fun createExpandedTileBitmap(tile: Tile, originalBitmap: Bitmap): Bitmap? {
        return tryOrDefault(null) {
            val borderSize = TILE_BORDER_PIXELS
            val expandedWidth = originalBitmap.width + (borderSize * 2)
            val expandedHeight = originalBitmap.height + (borderSize * 2)

            val expandedBitmap =
                if (false && cachedExpandedBitmap?.width == expandedWidth && cachedExpandedBitmap?.height == expandedHeight) {
                    cachedExpandedBitmap!!
                } else {
                    cachedExpandedBitmap?.recycle()
                    createBitmap(expandedWidth, expandedHeight).also {
                        cachedExpandedBitmap = it
                    }
                }

            val canvas = Canvas(expandedBitmap)

            // Draw center tile
            canvas.drawBitmap(
                originalBitmap,
                borderSize.toFloat(),
                borderSize.toFloat(),
                expandedBitmapPaint
            )

            // Helper function to draw a neighbor using bitmap shader
            fun drawNeighbor(
                neighborTile: Tile,
                destX: Float,
                destY: Float,
                destWidth: Int,
                destHeight: Int,
                srcXStart: Int,
                srcYStart: Int,
                srcWidth: Int,
                srcHeight: Int
            ) {
                loader.tileCache[neighborTile]?.let { neighborBitmap ->
                    val shader =
                        BitmapShader(neighborBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

                    // Calculate scale factors to map source region to destination
                    val scaleX = destWidth.toFloat() / srcWidth
                    val scaleY = destHeight.toFloat() / srcHeight

                    // Scale and translate to show the correct region
                    neighborMatrix.reset()
                    neighborMatrix.setScale(scaleX, scaleY)
                    neighborMatrix.postTranslate(
                        destX - srcXStart * scaleX,
                        destY - srcYStart * scaleY
                    )

                    shader.setLocalMatrix(neighborMatrix)
                    expandedBitmapPaint.shader = shader
                    canvas.drawRect(
                        destX,
                        destY,
                        destX + destWidth,
                        destY + destHeight,
                        expandedBitmapPaint
                    )
                    expandedBitmapPaint.shader = null
                }
            }

            // Draw neighboring tiles' edges
            // Top neighbor
            val topTile = Tile(tile.x, tile.y - 1, tile.z, tile.size)
            drawNeighbor(
                topTile,
                borderSize.toFloat(),
                0f,
                originalBitmap.width,
                borderSize,
                0,
                originalBitmap.height - borderSize,
                originalBitmap.width,
                borderSize
            )

            // Bottom neighbor
            val bottomTile = Tile(tile.x, tile.y + 1, tile.z, tile.size)
            drawNeighbor(
                bottomTile,
                borderSize.toFloat(),
                (originalBitmap.height + borderSize).toFloat(),
                originalBitmap.width,
                borderSize,
                0,
                0,
                originalBitmap.width,
                borderSize
            )

            // Left neighbor
            val leftTile = Tile(tile.x - 1, tile.y, tile.z, tile.size)
            drawNeighbor(
                leftTile,
                0f,
                borderSize.toFloat(),
                borderSize,
                originalBitmap.height,
                originalBitmap.width - borderSize,
                0,
                borderSize,
                originalBitmap.height
            )

            // Right neighbor
            val rightTile = Tile(tile.x + 1, tile.y, tile.z, tile.size)
            drawNeighbor(
                rightTile,
                (originalBitmap.width + borderSize).toFloat(),
                borderSize.toFloat(),
                borderSize,
                originalBitmap.height,
                0,
                0,
                borderSize,
                originalBitmap.height
            )

            // Top-left corner
            val topLeftTile = Tile(tile.x - 1, tile.y - 1, tile.z, tile.size)
            drawNeighbor(
                topLeftTile,
                0f,
                0f,
                borderSize,
                borderSize,
                originalBitmap.width - borderSize,
                originalBitmap.height - borderSize,
                borderSize,
                borderSize
            )

            // Top-right corner
            val topRightTile = Tile(tile.x + 1, tile.y - 1, tile.z, tile.size)
            drawNeighbor(
                topRightTile,
                (originalBitmap.width + borderSize).toFloat(),
                0f,
                borderSize,
                borderSize,
                0,
                originalBitmap.height - borderSize,
                borderSize,
                borderSize
            )

            // Bottom-left corner
            val bottomLeftTile = Tile(tile.x - 1, tile.y + 1, tile.z, tile.size)
            drawNeighbor(
                bottomLeftTile,
                0f,
                (originalBitmap.height + borderSize).toFloat(),
                borderSize,
                borderSize,
                originalBitmap.width - borderSize,
                0,
                borderSize,
                borderSize
            )

            // Bottom-right corner
            val bottomRightTile = Tile(tile.x + 1, tile.y + 1, tile.z, tile.size)
            drawNeighbor(
                bottomRightTile,
                (originalBitmap.width + borderSize).toFloat(),
                (originalBitmap.height + borderSize).toFloat(),
                borderSize,
                borderSize,
                0,
                0,
                borderSize,
                borderSize
            )

            expandedBitmap
        }
    }

    private fun getTiles(bounds: CoordinateBounds, projection: IMapViewProjection): List<Tile> {
        val zoom = TileMath.getZoomLevel(bounds, projection.metersPerPixel)
        var adjustedOffset = zoomOffset + 1
        var tiles: List<Tile>
        do {
            adjustedOffset--
            tiles = TileMath.getTiles(bounds, (zoom + adjustedOffset).coerceAtMost(20))
        } while (tiles.size > MAX_TILES && (zoom + adjustedOffset) > 1)
        return tiles
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }

    override fun start() {
        shouldReloadTiles = true
    }

    override fun stop() {
        taskRunner.stop()
        loader.clearCache()
        synchronized(cachedBitmapLock) {
            cachedExpandedBitmap?.recycle()
            cachedExpandedBitmap = null
        }
    }

    override fun setPreferences(preferences: Bundle) {
        percentOpacity = preferences.getInt(
            DefaultMapLayerDefinitions.OPACITY,
            DefaultMapLayerDefinitions.DEFAULT_OPACITY
        ) / 100f
    }

    override var percentOpacity: Float = 1f

    companion object {
        const val MAX_TILES = 150
        private const val TILE_BORDER_PIXELS = 2
    }
}