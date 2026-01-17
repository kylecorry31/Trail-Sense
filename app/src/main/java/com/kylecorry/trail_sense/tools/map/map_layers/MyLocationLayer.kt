package com.kylecorry.trail_sense.tools.map.map_layers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.os.Bundle
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.sensors.SensorService

class MyLocationLayer : GeoJsonLayer<MyLocationGeoJsonSource>(
    MyLocationGeoJsonSource(),
    layerId = LAYER_ID
) {
    private val _showDirection = AppServiceRegistry.get<SensorService>().hasCompass()
    private var _drawAccuracy: Boolean = true
    private var isInitialized = false
    private var _bitmap: Bitmap? = null

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        _drawAccuracy = preferences.getBoolean(SHOW_ACCURACY, DEFAULT_SHOW_ACCURACY)
    }

    override fun draw(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        if (!isInitialized) {
            val color = Resources.getPrimaryMarkerColor(context)
            source.setStyle(
                color,
                Resources.getPrimaryMarkerColor(context),
                _drawAccuracy,
                _showDirection,
                if (_showDirection) getArrowBitmap(context, drawer, color) else null
            )
            isInitialized = true
        }

        super.draw(context, drawer, map)
    }

    private fun getArrowBitmap(context: Context, drawer: ICanvasDrawer, color: Int): Bitmap {
        if (_bitmap != null) {
            return _bitmap!!
        }

        val size = drawer.dp(16f).toInt()
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val bitmapDrawer = CanvasDrawer(context, canvas)

        val path = Path()
        // Bottom Left
        path.moveTo(size * 0.1f, size.toFloat())

        // Top
        path.lineTo(size / 2f, 0f)

        // Bottom right
        path.lineTo(size * 0.9f, size.toFloat())

        // Middle dip
        path.lineTo(size / 2f, size * 0.8f)

        path.close()
        bitmapDrawer.push()
        bitmapDrawer.fill(color)
        bitmapDrawer.stroke(Color.WHITE)
        bitmapDrawer.strokeWeight(2f)
        bitmapDrawer.path(path)
        bitmapDrawer.pop()

        _bitmap = bitmap
        return bitmap
    }

    protected fun finalize() {
        _bitmap?.recycle()
        _bitmap = null
    }

    companion object {
        const val LAYER_ID = "my_location"
        const val SHOW_ACCURACY = "show_accuracy"
        const val DEFAULT_SHOW_ACCURACY = true
    }
}
