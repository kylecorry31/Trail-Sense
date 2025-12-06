package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import android.graphics.Paint
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.roundNearest
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMap
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMapFactory
import com.kylecorry.trail_sense.shared.dem.colors.USGSElevationColorMap
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel

class ElevationLayer(private val taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()) :
    IAsyncLayer {
    private var updateListener: (() -> Unit)? = null

    init {
        taskRunner.addTask { bounds, metersPerPixel ->
            val zoomLevel = TileMath.distancePerPixelToZoom(
                metersPerPixel.toDouble(),
                (bounds.north + bounds.south) / 2
            ).coerceAtMost(maxZoomLevel)

            if (zoomLevel < minZoomLevel) {
                return@addTask
            }

            val newElevation =
                DEM.elevationImage(
                    bounds,
                    validResolutions[zoomLevel]!!
                ) { elevation, _, maxElevation ->
                    if (useDynamicElevationScale) {
                        var max = (maxElevation * 1.25f).roundNearest(1000f)
                        if (max < maxElevation) {
                            max += 1000f
                        }
                        colorScale.getColor(
                            SolMath.norm(
                                elevation,
                                minScaleElevation,
                                max,
                                true
                            )
                        )
                    } else {
                        colorScale.getElevationColor(elevation)
                    }
                }
            synchronized(elevationLock) {
                elevation?.recycle()
                elevation = newElevation
                elevationBounds = bounds
            }
            lastZoomLevel = zoomLevel
            onMain {
                updateListener?.invoke()
            }
        }
    }

    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    // TODO: Extract this for use by all contour type layers
    private val minZoomLevel = 10
    private val maxZoomLevel = 19
    private val baseResolution = 1 / 240.0
    private val validResolutions = mapOf(
        10 to baseResolution * 8,
        11 to baseResolution * 4,
        12 to baseResolution * 2,
        13 to baseResolution,
        14 to baseResolution / 2,
        15 to baseResolution / 4,
        16 to baseResolution / 4,
        17 to baseResolution / 4,
        18 to baseResolution / 4,
        19 to baseResolution / 4
    )

    private var elevationLock = Any()
    private var elevation: Bitmap? = null
    private var elevationBounds: CoordinateBounds? = null
    private var lastZoomLevel = -1

    private var colorScale: ElevationColorMap = USGSElevationColorMap()

    private val useDynamicElevationScale = false
    private val minScaleElevation = 0f

    fun setPreferences(prefs: ElevationMapLayerPreferences) {
        _percentOpacity = prefs.opacity.get() / 100f
        colorScale = ElevationColorMapFactory().getElevationColorMap(prefs.colorStrategy.get())
        invalidate()
    }

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        if (SafeMode.isEnabled()) {
            return
        }

        taskRunner.scheduleUpdate(map.mapBounds, map.metersPerPixel)

        synchronized(elevationLock) {
            val elevation = this@ElevationLayer.elevation ?: return@synchronized
            val elevationBounds = this@ElevationLayer.elevationBounds ?: return@synchronized
            // TODO: This moves around when it loads new bounds
            val topLeftPixel = map.toPixel(elevationBounds.northWest)
            val topRightPixel = map.toPixel(elevationBounds.northEast)
            val bottomRightPixel = map.toPixel(elevationBounds.southEast)
            val bottomLeftPixel = map.toPixel(elevationBounds.southWest)
            drawer.canvas.drawBitmapMesh(
                elevation,
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

    private var _percentOpacity: Float = 1f

    override val percentOpacity: Float
        get() = _percentOpacity

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }
}