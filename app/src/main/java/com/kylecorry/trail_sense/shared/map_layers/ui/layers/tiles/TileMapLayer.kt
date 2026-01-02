package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.setBlendMode
import com.kylecorry.andromeda.canvas.ICanvasDrawer
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
    protected val loader = TileLoader(TILE_BORDER_PIXELS)
    private val layerPaint = Paint()
    private val tilePaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = true
    }
    private val neighborPaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
    }
    var shouldMultiply = false
        set(value) {
            field = value
            if (value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                layerPaint.setBlendMode(BlendModeCompat.MULTIPLY)
            } else {
                layerPaint.setBlendMode(BlendModeCompat.SRC_OVER)
            }
        }
    var multiplyAlpha: Int = 255
        set(value) {
            field = value
            layerPaint.alpha = value
        }
    private var updateListener: (() -> Unit)? = null
    private var zoomOffset: Int = 0
    private val renderMatrix = Matrix()
    private val srcPoints = FloatArray(8)
    private val dstPoints = FloatArray(8)
    private val srcRect = Rect()
    private val destRect = Rect()

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
            var shouldSaveLayer = false
            try {
                if (shouldMultiply) {
                    drawer.canvas.saveLayer(null, layerPaint)
                    shouldSaveLayer = true
                }
                renderTiles(drawer.canvas, map)
            } finally {
                if (shouldSaveLayer) {
                    drawer.pop()
                }
            }
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
            renderTile(
                tile,
                canvas,
                map,
                bitmap
            )
        }
    }

    private fun isTooSmall(
        topLeft: PixelCoordinate,
        topRight: PixelCoordinate,
        bottomLeft: PixelCoordinate,
        bottomRight: PixelCoordinate
    ): Boolean {
        val minSize = 10f
        val width = max(topRight.x, bottomRight.x) - min(topLeft.x, bottomLeft.x)
        val height = max(bottomLeft.y, bottomRight.y) - min(topLeft.y, topRight.y)
        return width < minSize || height < minSize
    }

    private fun isFarOffScreen(
        topLeft: PixelCoordinate,
        topRight: PixelCoordinate,
        bottomLeft: PixelCoordinate,
        bottomRight: PixelCoordinate,
        canvasWidth: Int,
        canvasHeight: Int
    ): Boolean {
        val buffer = canvasHeight.coerceAtLeast(canvasWidth) * 2
        val minX = minOf(topLeft.x, bottomLeft.x)
        val maxX = maxOf(topRight.x, bottomRight.x)
        val minY = minOf(topLeft.y, topRight.y)
        val maxY = maxOf(bottomLeft.y, bottomRight.y)

        return maxX < -buffer ||
                minX > canvasWidth + buffer ||
                maxY < -buffer ||
                minY > canvasHeight + buffer
    }

    private fun renderTile(
        tile: Tile,
        canvas: Canvas,
        map: IMapView,
        bitmap: Bitmap
    ) {
        val bounds = tile.getBounds()
        val topLeftPixel = map.toPixel(bounds.northWest)
        val topRightPixel = map.toPixel(bounds.northEast)
        val bottomRightPixel = map.toPixel(bounds.southEast)
        val bottomLeftPixel = map.toPixel(bounds.southWest)

        if (isTooSmall(topLeftPixel, topRightPixel, bottomLeftPixel, bottomRightPixel)) {
            return
        }

        if (isFarOffScreen(
                topLeftPixel,
                topRightPixel,
                bottomLeftPixel,
                bottomRightPixel,
                canvas.width,
                canvas.height
            )
        ) {
            return
        }

        fillNeighborPixels(tile, bitmap)

        val borderPixels = TILE_BORDER_PIXELS

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

        canvas.save()
        canvas.concat(renderMatrix)

        srcRect.set(
            borderPixels,
            borderPixels,
            bitmap.width - borderPixels,
            bitmap.height - borderPixels
        )
        destRect.set(
            borderPixels,
            borderPixels,
            bitmap.width - borderPixels,
            bitmap.height - borderPixels
        )

        canvas.drawBitmap(bitmap, srcRect, destRect, tilePaint)
        canvas.restore()
    }

    private fun drawNeighbor(
        canvas: Canvas,
        neighborTile: Tile,
        destX: Int,
        destY: Int,
        destWidth: Int,
        destHeight: Int,
        srcXStart: Int,
        srcYStart: Int
    ) {
        loader.tileCache[neighborTile]?.let { neighborBitmap ->
            srcRect.set(
                srcXStart,
                srcYStart,
                srcXStart + destWidth,
                srcYStart + destHeight
            )

            destRect.set(
                destX,
                destY,
                (destX + destWidth),
                (destY + destHeight)
            )

            canvas.drawBitmap(
                neighborBitmap,
                srcRect,
                destRect,
                neighborPaint
            )
        }
    }

    private fun fillNeighborPixels(tile: Tile, originalBitmap: Bitmap) {
        val borderSize = TILE_BORDER_PIXELS

        val canvas = Canvas(originalBitmap)

        val topTile = Tile(tile.x, tile.y - 1, tile.z, tile.size)
        drawNeighbor(
            canvas,
            topTile,
            borderSize,
            0,
            originalBitmap.width,
            borderSize,
            borderSize,
            originalBitmap.height - borderSize * 2
        )

        val bottomTile = Tile(tile.x, tile.y + 1, tile.z, tile.size)
        drawNeighbor(
            canvas,
            bottomTile,
            borderSize,
            originalBitmap.height - borderSize,
            originalBitmap.width,
            borderSize,
            borderSize,
            borderSize
        )

        val leftTile = Tile(tile.x - 1, tile.y, tile.z, tile.size)
        drawNeighbor(
            canvas,
            leftTile,
            0,
            borderSize,
            borderSize,
            originalBitmap.height,
            originalBitmap.width - borderSize * 2,
            borderSize
        )

        val rightTile = Tile(tile.x + 1, tile.y, tile.z, tile.size)
        drawNeighbor(
            canvas,
            rightTile,
            originalBitmap.width - borderSize,
            borderSize,
            borderSize,
            originalBitmap.height,
            borderSize,
            borderSize
        )

        val topLeftTile = Tile(tile.x - 1, tile.y - 1, tile.z, tile.size)
        drawNeighbor(
            canvas,
            topLeftTile,
            0,
            0,
            borderSize,
            borderSize,
            originalBitmap.width - borderSize * 2,
            originalBitmap.height - borderSize * 2
        )

        val topRightTile = Tile(tile.x + 1, tile.y - 1, tile.z, tile.size)
        drawNeighbor(
            canvas,
            topRightTile,
            originalBitmap.width - borderSize,
            0,
            borderSize,
            borderSize,
            borderSize,
            originalBitmap.height - borderSize * 2
        )

        val bottomLeftTile = Tile(tile.x - 1, tile.y + 1, tile.z, tile.size)
        drawNeighbor(
            canvas,
            bottomLeftTile,
            0,
            originalBitmap.height - borderSize,
            borderSize,
            borderSize,
            originalBitmap.width - borderSize * 2,
            borderSize
        )

        val bottomRightTile = Tile(tile.x + 1, tile.y + 1, tile.z, tile.size)
        drawNeighbor(
            canvas,
            bottomRightTile,
            originalBitmap.width - borderSize,
            originalBitmap.height - borderSize,
            borderSize,
            borderSize,
            borderSize,
            borderSize
        )
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