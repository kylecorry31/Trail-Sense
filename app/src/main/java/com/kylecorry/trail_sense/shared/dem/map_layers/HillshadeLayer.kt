package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import android.graphics.Paint
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.andromeda_temp.withLayerOpacity
import com.kylecorry.trail_sense.shared.canvas.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView

class HillshadeLayer : IAsyncLayer {
    private var updateListener: (() -> Unit)? = null

    private val taskRunner = MapLayerBackgroundTask()
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    // TODO: Extract this for use by all contour type layers
    private val minZoomLevel = 13
    private val maxZoomLevel = 19
    private val baseResolution = 1 / 240.0
    private val validResolutions = mapOf(
        13 to baseResolution,
        14 to baseResolution / 2,
        15 to baseResolution / 4,
        16 to baseResolution / 4,
        17 to baseResolution / 4,
        18 to baseResolution / 4,
        19 to baseResolution / 4
    )

    private var hillshadeLock = Any()
    private var hillshade: Bitmap? = null
    private var hillshadeBounds: CoordinateBounds? = null
    private var lastZoomLevel = -1

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        if (SafeMode.isEnabled() || map.metersPerPixel > 75f) {
            return
        }

        taskRunner.scheduleUpdate(map.mapBounds, map.metersPerPixel) { bounds, metersPerPixel ->
            val zoomLevel = TileMath.distancePerPixelToZoom(
                metersPerPixel.toDouble(),
                (bounds.north + bounds.south) / 2
            ).coerceAtMost(maxZoomLevel)

            if (zoomLevel < minZoomLevel) {
                return@scheduleUpdate
            }

            val newHillshade = DEM.hillshadeImage(bounds, validResolutions[zoomLevel]!!)
            synchronized(hillshadeLock) {
                hillshade?.recycle()
                hillshade = newHillshade
                hillshadeBounds = bounds
            }
            lastZoomLevel = zoomLevel
            updateListener?.invoke()
        }

        drawer.withLayerOpacity(opacity) {
            synchronized(hillshadeLock) {
                val hillshade = hillshade ?: return@synchronized
                val hillshadeBounds = hillshadeBounds ?: return@synchronized
                val topLeftPixel = map.toPixel(hillshadeBounds.northWest)
                val topRightPixel = map.toPixel(hillshadeBounds.northEast)
                val bottomRightPixel = map.toPixel(hillshadeBounds.southEast)
                val bottomLeftPixel = map.toPixel(hillshadeBounds.southWest)
                drawer.canvas.drawBitmapMesh(
                    hillshade,
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

    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    // TODO: Set via preferences
    private var _percentOpacity: Float = 0.5f

    override val percentOpacity: Float
        get() = _percentOpacity

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }
}