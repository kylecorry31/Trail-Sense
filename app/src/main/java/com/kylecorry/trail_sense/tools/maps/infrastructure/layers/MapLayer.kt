package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.tiles.TileLoader
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class MapLayer : ILayer {

    private var shouldReloadTiles = true
    private var maps: List<PhotoMap> = emptyList()
    private var opacity: Int = 255
    private var replaceWhitePixels: Boolean = false
    private var lastBounds: CoordinateBounds = CoordinateBounds.empty
    private var lastMetersPerPixel: Float? = null
    private val runner = CoroutineQueueRunner(2)
    private val scope = CoroutineScope(Dispatchers.Default)
    private val loader = TileLoader()

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

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
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
                        loader.loadTiles(maps, bounds, lastMetersPerPixel ?: 0f, replaceWhitePixels)
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
        // TODO: If the user zooms way in before tiles load, the bitmaps may be too big for bitmapMesh (try that out)
        synchronized(loader.lock) {
            val bitmap = createBitmap(drawer.canvas.width, drawer.canvas.height)
            try {
                val canvas = Canvas(bitmap)
                val paint = Paint().apply {
                    isAntiAlias = true
                }
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
                            paint
                        )
                    }
                }

                drawer.opacity(opacity)
                drawer.image(bitmap, 0f, 0f)
                drawer.opacity(255)
            } finally {
                bitmap.recycle()
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

    private fun isRelativelyCloseTo(
        a: Float,
        b: Float,
        threshold: Float = 0.1f
    ): Boolean {
        return percentDifference(a, b) <= threshold
    }

    private fun percentDifference(a: Float, b: Float): Float {
        return if (a == 0f && b == 0f) {
            0f
        } else {
            ((b - a) / a).absoluteValue
        }
    }

    private fun areBoundsEqual(bounds1: CoordinateBounds, bound2: CoordinateBounds): Boolean {
        return bounds1.north == bound2.north &&
                bounds1.south == bound2.south &&
                bounds1.east == bound2.east &&
                bounds1.west == bound2.west
    }
}