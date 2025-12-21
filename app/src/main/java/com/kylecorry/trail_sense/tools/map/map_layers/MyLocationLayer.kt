package com.kylecorry.trail_sense.tools.map.map_layers

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.os.Bundle
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.navigation.ui.markers.CircleMapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.PathMapMarker

class MyLocationLayer : IAsyncLayer {

    override val layerId: String = LAYER_ID

    private var _location: Coordinate? = null
    private var _azimuth: Float? = null
    private var _path: Path? = null
    private val _showDirection = AppServiceRegistry.get<SensorService>().hasCompass()

    @ColorInt
    private var _color: Int = Color.WHITE


    private var _accuracy: Float? = null

    private var _drawAccuracy: Boolean = true

    @ColorInt
    private var _accuracyFillColor: Int = Color.WHITE

    private var updateListener: (() -> Unit)? = null
    private var _percentOpacity: Float = 1f

    override val percentOpacity: Float
        get() = _percentOpacity

    init {
        val context = AppServiceRegistry.get<Context>()
        _color = Resources.getPrimaryMarkerColor(context)
        _accuracyFillColor = Resources.getPrimaryMarkerColor(context)
    }

    fun setLocation(location: Coordinate) {
        _location = location
        invalidate()
    }

    fun setAccuracy(accuracy: Float?) {
        _accuracy = accuracy
        invalidate()
    }

    fun setAzimuth(azimuth: Float) {
        _azimuth = azimuth
        invalidate()
    }

    fun setColor(@ColorInt color: Int) {
        _color = color
        invalidate()
    }

    fun setAccuracyColor(@ColorInt color: Int) {
        _accuracyFillColor = color
        invalidate()
    }

    override fun setPreferences(preferences: Bundle) {
        _percentOpacity = preferences.getInt(DefaultMapLayerDefinitions.OPACITY) / 100f
        _drawAccuracy = preferences.getBoolean(SHOW_ACCURACY)
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        if (_drawAccuracy) {
            drawAccuracy(drawer, map)
        }
        if (_showDirection) {
            drawArrow(drawer, map)
        } else {
            drawCircle(drawer, map)
        }
    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {
        updateListener?.invoke()
    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        return false
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }

    private fun drawAccuracy(drawer: ICanvasDrawer, map: IMapView) {
        val accuracy = _accuracy ?: return
        val location = _location ?: return
        if (map.metersPerPixel <= 0) return

        val sizePixels = 2 * accuracy / map.metersPerPixel * map.layerScale
        val sizeDp = sizePixels / drawer.dp(1f)

        val marker = CircleMapMarker(
            location,
            _accuracyFillColor,
            null,
            25,
            sizeDp
        )

        val anchor = map.toPixel(marker.location)
        marker.draw(
            drawer,
            anchor,
            map.layerScale,
            map.mapAzimuth + map.mapRotation,
            map.metersPerPixel
        )
    }

    private fun drawCircle(drawer: ICanvasDrawer, map: IMapView) {
        val marker = CircleMapMarker(
            _location ?: map.mapCenter,
            color = _color,
            strokeColor = Color.WHITE,
            strokeWeight = 2f,
            size = 16f
        )
        val anchor = map.toPixel(marker.location)
        marker.draw(
            drawer,
            anchor,
            map.layerScale,
            map.mapAzimuth + map.mapRotation,
            map.metersPerPixel
        )
    }

    private fun drawArrow(drawer: ICanvasDrawer, map: IMapView) {
        val path = _path ?: Path()
        if (_path == null) {
            val size = drawer.dp(16f)
            // Bottom Left
            path.moveTo(-size / 2.5f, size / 2f)

            // Top
            path.lineTo(0f, -size / 2f)

            // Bottom right
            path.lineTo(size / 2.5f, size / 2f)

            // Middle dip
            path.lineTo(0f, size / 3f)

            path.close()

            _path = path
        }

        val marker = PathMapMarker(
            _location ?: map.mapCenter,
            path,
            size = 16f,
            color = _color,
            strokeColor = Color.WHITE,
            strokeWeight = 2f,
            rotation = (_azimuth ?: 0f) + map.mapRotation
        )

        val anchor = map.toPixel(marker.location)
        marker.draw(
            drawer,
            anchor,
            map.layerScale,
            map.mapAzimuth + map.mapRotation,
            map.metersPerPixel
        )
    }

    protected fun finalize() {
        _path = null
    }

    companion object {
        const val LAYER_ID = "my_location"
        const val SHOW_ACCURACY = "show_accuracy"
    }
}
