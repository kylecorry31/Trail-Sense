package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import android.util.Size
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.tiles.TileLoader
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

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
            val bounds = getBounds(drawer)
            val viewportSize = Size(bounds.width().toInt(), bounds.height().toInt())
            isInvalid = false
            lastBounds?.let {
                scope.launch {
                    runner.replace {
                        loader.loadTiles(maps, it, viewportSize)
                    }
                }
            }
        }

        // Render loaded tiles
        synchronized(loader.lock) {
            loader.tileCache.forEach { (tileBounds, bitmaps) ->
                bitmaps.reversed().forEach { bitmap ->
                    // TODO: There are small gaps
                    val topLeftPixel = map.toPixel(tileBounds.northWest)
                    val bottomRightPixel = map.toPixel(tileBounds.southEast)
                    drawer.opacity(opacity)
                    drawer.image(
                        bitmap,
                        min(topLeftPixel.x, bottomRightPixel.x),
                        min(topLeftPixel.y, bottomRightPixel.y),
                        abs(bottomRightPixel.x - topLeftPixel.x),
                        abs(bottomRightPixel.y - topLeftPixel.y)
                    )
                    drawer.opacity(255)
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
}