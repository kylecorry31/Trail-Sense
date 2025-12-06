package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Build
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.setBlendMode
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel

class HillshadeLayer(private val taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()) :
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

            val newHillshade = DEM.hillshadeImage(bounds, validResolutions[zoomLevel]!!, 3f)
            synchronized(hillshadeLock) {
                hillshade?.recycle()
                hillshade = newHillshade
                hillshadeBounds = bounds
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
        alpha = 127
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setBlendMode(BlendModeCompat.MULTIPLY)
        }
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

    private var hillshadeLock = Any()
    private var hillshade: Bitmap? = null
    private var hillshadeBounds: CoordinateBounds? = null
    private var lastZoomLevel = -1

    fun setPreferences(prefs: HillshadeMapLayerPreferences) {
        paint.alpha = SolMath.map(
            prefs.opacity.get() / 100f,
            0f,
            1f,
            0f,
            255f,
            shouldClamp = true
        ).toInt()
        invalidate()
    }

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        if (SafeMode.isEnabled()) {
            return
        }

        // TODO: Add a shared task runner which each layer registers with that allows it to load using the same bounds
        taskRunner.scheduleUpdate(map.mapBounds, map.metersPerPixel)

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

    override val percentOpacity: Float = 1f

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }
}