package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask2
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileLoader
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileSource
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import kotlinx.coroutines.CancellationException
import kotlin.math.hypot

abstract class TileMapLayer<T : TileSource>(
    protected val source: T,
    private val taskRunner: MapLayerBackgroundTask2 = MapLayerBackgroundTask2(),
    private val minZoomLevel: Int? = null,
) : IAsyncLayer {

    private var shouldReloadTiles = true
    private var backgroundColor: Int = Color.WHITE
    protected val loader = TileLoader()
    protected var preRenderBitmaps: Boolean = false
    protected val tilePaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    var alpha: Int = 255
    private var updateListener: (() -> Unit)? = null
    private var preRenderedBitmap: Bitmap? = null
    private var preRenderedBounds: CoordinateBounds? = null

    open fun setBackgroundColor(color: Int) {
        this.backgroundColor = color
        shouldReloadTiles = true
    }

    init {
        // Load tiles if needed
        taskRunner.addTask { viewBounds: Rectangle, bounds: CoordinateBounds, projection: IMapViewProjection ->
            shouldReloadTiles = false
            try {
                val tiles = TileMath.getTiles(bounds, projection.metersPerPixel.toDouble())

                if (tiles.size <= MAX_TILES &&
                    (tiles.firstOrNull()?.z ?: 0) >= (minZoomLevel ?: 0)
                ) {
                    loader.loadTiles(source, sortTiles(tiles))
                } else if (tiles.size > MAX_TILES) {
                    Log.d("TileLoader", "Too many tiles to load: ${tiles.size}")
                }

                if (preRenderBitmaps) {
                    preRenderTiles()
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

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        return false
    }

    private fun renderTiles(canvas: Canvas, map: IMapView) {
        if (preRenderedBitmap != null && preRenderedBounds != null) {
            renderTile(canvas, map, preRenderedBounds ?: return, preRenderedBitmap ?: return)
            return
        }

        loader.tileCache.entries.sortedBy { it.key.z }.forEach { (tile, bitmaps) ->
            val tileBounds = tile.getBounds()
            bitmaps.reversed().forEach { bitmap ->
                renderTile(canvas, map, tileBounds, bitmap)
            }
        }
    }

    private fun preRenderTiles() {
        synchronized(loader.lock) {
            if (loader.tileCache.isEmpty()) {
                return
            }

            // Calculate tile grid dimensions and collect bitmap dimensions
            var minTileX = Int.MAX_VALUE
            var minTileY = Int.MAX_VALUE
            var maxTileX = Int.MIN_VALUE
            var maxTileY = Int.MIN_VALUE
            var tileWidth = 0
            var tileHeight = 0

            loader.tileCache.forEach { (tile, bitmaps) ->
                minTileX = minOf(minTileX, tile.x)
                maxTileX = maxOf(maxTileX, tile.x)
                minTileY = minOf(minTileY, tile.y)
                maxTileY = maxOf(maxTileY, tile.y)
                if (bitmaps.isNotEmpty()) {
                    tileWidth = bitmaps[0].width
                    tileHeight = bitmaps[0].height
                }
            }

            preRenderedBitmap?.recycle()

            val tilesWide = (maxTileX - minTileX) + 1
            val tilesHigh = (maxTileY - minTileY) + 1
            val bitmapWidth = tilesWide * tileWidth
            val bitmapHeight = tilesHigh * tileHeight

            // Don't pre-render large bitmaps
            if (bitmapWidth > MAX_PRE_RENDER_SIZE || bitmapHeight > MAX_PRE_RENDER_SIZE) {
                preRenderedBitmap = null
                preRenderedBounds = null
                return
            }

            preRenderedBitmap = createBitmap(bitmapWidth, bitmapHeight)
            val mergeCanvas = Canvas(preRenderedBitmap!!)
            mergeCanvas.drawColor(Color.TRANSPARENT)

            // Render all tiles into the merged bitmap
            loader.tileCache.entries.sortedBy { it.key.z }.forEach { (tile, bitmaps) ->
                val pixelX = (tile.x - minTileX) * tileWidth
                val pixelY = (maxTileY - tile.y) * tileHeight
                bitmaps.reversed().forEach { bitmap ->
                    mergeCanvas.drawBitmap(bitmap, pixelX.toFloat(), pixelY.toFloat(), null)
                }
            }

            // Calculate geographic bounds
            preRenderedBounds = CoordinateBounds.from(
                loader.tileCache.keys.flatMap { tile ->
                    val tileBounds = tile.getBounds()
                    listOf(tileBounds.northWest, tileBounds.southEast)
                }
            )
        }
    }

    private fun renderTile(
        canvas: Canvas,
        map: IMapView,
        bounds: CoordinateBounds,
        bitmap: Bitmap
    ) {
        val topLeftPixel = map.toPixel(bounds.northWest)
        val topRightPixel = map.toPixel(bounds.northEast)
        val bottomRightPixel = map.toPixel(bounds.southEast)
        val bottomLeftPixel = map.toPixel(bounds.southWest)
        canvas.drawBitmapMesh(
            bitmap,
            1,
            1,
            floatArrayOf(
                // Intentionally inverted along the Y axis
                bottomLeftPixel.x, bottomLeftPixel.y,
                bottomRightPixel.x, bottomRightPixel.y,
                topLeftPixel.x, topLeftPixel.y,
                topRightPixel.x, topRightPixel.y
            ),
            0,
            null,
            0,
            tilePaint
        )
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }

    override var percentOpacity: Float = 1f

    companion object {
        private const val MAX_PRE_RENDER_SIZE = 500
        private const val MAX_TILES = 100
    }
}