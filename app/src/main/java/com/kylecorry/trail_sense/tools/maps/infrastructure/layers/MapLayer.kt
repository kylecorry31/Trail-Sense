package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import android.graphics.Paint
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

class MapLayer : ILayer {

    private var isInvalid = true
    private var maps: List<PhotoMap> = emptyList()
    private var opacity: Int = 255
    private var lastBounds: CoordinateBounds? = null
    private val runner = CoroutineQueueRunner()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val loader = TileLoader()

    fun setMaps(maps: List<PhotoMap>) {
        this.maps = maps
        loader.clearCache()
        invalidate()
    }

    fun setBounds(bounds: CoordinateBounds?) {
        if (bounds == lastBounds) {
            return
        }
        lastBounds = bounds
        invalidate()
    }

    fun setOpacity(opacity: Int) {
        this.opacity = opacity
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        // Load tiles if needed
        if (isInvalid && lastBounds != null) {
            isInvalid = false
            lastBounds?.let {
                scope.launch {
                    runner.replace {
                        try {
                            loader.loadTiles(maps, it, map.metersPerPixel)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            isInvalid = true
                        }
                    }
                }
            }
        }

        // Render loaded tiles
        synchronized(loader.lock) {
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                isDither = true
                alpha = opacity
            }
            loader.tileCache.forEach { (tile, bitmaps) ->
                val tileBounds = tile.getBounds()
                bitmaps.reversed().forEach { bitmap ->
                    val topLeftPixel = map.toPixel(tileBounds.northWest)
                    val topRightPixel = map.toPixel(tileBounds.northEast)
                    val bottomRightPixel = map.toPixel(tileBounds.southEast)
                    val bottomLeftPixel = map.toPixel(tileBounds.southWest)
                    drawer.canvas.drawBitmapMesh(
                        bitmap,
                        1,
                        1,
                        floatArrayOf(
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
}