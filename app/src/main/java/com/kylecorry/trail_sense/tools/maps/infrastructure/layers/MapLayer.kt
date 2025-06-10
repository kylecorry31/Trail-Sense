package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import android.graphics.Canvas
import android.graphics.Paint
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.device.DeviceSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.tiles.TileLoader
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapLayer : ILayer {

    private var shouldReloadTiles = true
    private var maps: List<PhotoMap> = emptyList()
    private var opacity: Int = 255
    private var replaceWhitePixels: Boolean = false
    private var minZoom: Int = 0
    private var lastBounds: CoordinateBounds = CoordinateBounds.empty
    private var lastMetersPerPixel: Float? = null
    private val runner = CoroutineQueueRunner(2)
    private val scope = CoroutineScope(Dispatchers.Default)
    private val loader = TileLoader()
    private val tilePaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    fun setMaps(maps: List<PhotoMap>) {
        this.maps = maps
        loader.clearCache()
        shouldReloadTiles = true
    }

    fun setOpacity(opacity: Int) {
        this.opacity = opacity
    }

    fun setReplaceWhitePixels(replaceWhitePixels: Boolean) {
        this.replaceWhitePixels = replaceWhitePixels
        shouldReloadTiles = true
    }

    fun setMinZoom(minZoom: Int) {
        this.minZoom = minZoom
        shouldReloadTiles = true
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        // Avoid drawing while in safe mode
        if (SafeMode.isEnabled()) {
            return
        }

        // Load tiles if needed
        val bounds = map.mapBounds
        if (shouldReloadTiles || !areBoundsEqual(
                lastBounds,
                bounds
            ) || map.metersPerPixel != lastMetersPerPixel
        ) {
            shouldReloadTiles = false
            scope.launch {
                // TODO: Debounce loader
                runner.enqueue {
                    try {
                        loader.loadTiles(
                            maps,
                            bounds.grow(getGrowPercent()),
                            lastMetersPerPixel ?: 0f,
                            replaceWhitePixels,
                            minZoom
                        )
                    } catch (e: CancellationException) {
                        System.gc()
                        throw e
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        shouldReloadTiles = true
                    }
                }
            }
        }

        lastBounds = bounds
        lastMetersPerPixel = map.metersPerPixel

        // Render loaded tiles
        synchronized(loader.lock) {
            if (opacity == 255) {
                renderTiles(drawer.canvas, map)
            } else {
                drawer.canvas.saveLayerAlpha(null, opacity)
                renderTiles(drawer.canvas, map)
                drawer.canvas.restore()
            }
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

    private fun areBoundsEqual(bounds1: CoordinateBounds, bound2: CoordinateBounds): Boolean {
        return bounds1.north == bound2.north &&
                bounds1.south == bound2.south &&
                bounds1.east == bound2.east &&
                bounds1.west == bound2.west
    }

    private fun getGrowPercent(): Float {
        val device = AppServiceRegistry.get<DeviceSubsystem>()
        val threshold = 50 * 1024 * 1024 // 50 MB
        return if (device.getAvailableMemoryBytes() < threshold) {
            0f
        } else {
            0.5f
        }
    }

    private fun CoordinateBounds.grow(percent: Float): CoordinateBounds {
        val x = this.width() * percent
        val y = this.height() * percent
        return CoordinateBounds.from(
            listOf(
                northWest.plus(x, Bearing.from(CompassDirection.West))
                    .plus(y, Bearing.from(CompassDirection.North)),
                northEast.plus(x, Bearing.from(CompassDirection.East))
                    .plus(y, Bearing.from(CompassDirection.North)),
                southWest.plus(x, Bearing.from(CompassDirection.West))
                    .plus(y, Bearing.from(CompassDirection.South)),
                southEast.plus(x, Bearing.from(CompassDirection.East))
                    .plus(y, Bearing.from(CompassDirection.South)),
            )
        )
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
}