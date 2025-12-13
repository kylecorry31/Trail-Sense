package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask2
import com.kylecorry.trail_sense.shared.map_layers.tiles.ITileSourceSelector
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileLoader
import kotlinx.coroutines.CancellationException

abstract class TileMapLayer<T : ITileSourceSelector>(
    protected val source: T,
    private val taskRunner: MapLayerBackgroundTask2 = MapLayerBackgroundTask2(),
    private val minZoomLevel: Int? = null
) : IAsyncLayer {

    private var shouldReloadTiles = true
    private var backgroundColor: Int = Color.WHITE
    protected var controlsPdfCache = false
    private val loader = TileLoader()
    private val tilePaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    private var updateListener: (() -> Unit)? = null

    fun setBackgroundColor(color: Int) {
        this.backgroundColor = color
        shouldReloadTiles = true
    }

    init {
        // Load tiles if needed
        taskRunner.addTask { viewBounds: Rectangle, bounds: CoordinateBounds, projection: IMapViewProjection ->
            shouldReloadTiles = false
            try {
                loader.loadTiles(
                    source,
                    bounds,
                    projection.metersPerPixel,
                    minZoomLevel ?: 0,
                    backgroundColor,
                    controlsPdfCache
                )
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
            tilePaint.alpha = 255
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
        loader.tileCache.entries.sortedBy { it.key.z }.forEach { (tile, bitmaps) ->
            val tileBounds = tile.getBounds()
            bitmaps.reversed().forEach { bitmap ->
                val topLeftPixel = map.toPixel(tileBounds.northWest)
                val topRightPixel = map.toPixel(tileBounds.northEast)
                val bottomRightPixel = map.toPixel(tileBounds.southEast)
                val bottomLeftPixel = map.toPixel(tileBounds.southWest)
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
        }
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }

    override var percentOpacity: Float = 1f
}