package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Color
import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.ui.markers.PathMapMarker

class MyLocationLayer : BaseLayer() {

    private var _location: Coordinate? = null
    private var _azimuth: Bearing? = null
    private var _path: Path? = null

    @ColorInt
    private var _color: Int = Color.WHITE

    fun setLocation(location: Coordinate) {
        _location = location
        invalidate()
    }

    fun setAzimuth(azimuth: Bearing) {
        _azimuth = azimuth
        invalidate()
    }

    fun setColor(@ColorInt color: Int) {
        _color = color
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
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
        clearMarkers()
        addMarker(
            PathMapMarker(
                _location ?: map.mapCenter,
                path,
                size = 16f,
                color = _color,
                strokeColor = Color.WHITE,
                strokeWeight = 2f,
                rotation = (_azimuth?.value ?: 0f) + map.mapRotation
            )
        )
//        // TODO: Convert the drawable to a path
//        // Outline
//        addMarker(BitmapMapMarker(_location ?: map.mapCenter, image, 16f, (_azimuth?.value ?: 0f) + map.mapRotation, Color.WHITE))
//        // Fill
//        addMarker(BitmapMapMarker(_location ?: map.mapCenter, image, 12f, (_azimuth?.value ?: 0f) + map.mapRotation, _color))
        super.draw(drawer, map)
    }

    protected fun finalize() {
        _path = null
    }
}