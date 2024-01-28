package com.kylecorry.trail_sense.tools.navigation.ui.layers

import android.graphics.Color
import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.navigation.ui.markers.CircleMapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.PathMapMarker

class MyLocationLayer : BaseLayer() {

    private var _location: Coordinate? = null
    private var _azimuth: Float? = null
    private var _path: Path? = null
    private var _showDirection = true

    @ColorInt
    private var _color: Int = Color.WHITE

    fun setShowDirection(show: Boolean) {
        _showDirection = show
        invalidate()
    }

    fun setLocation(location: Coordinate) {
        _location = location
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

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        if (_showDirection) {
            drawArrow(drawer, map)
        } else {
            drawCircle(map)
        }
        super.draw(drawer, map)
    }

    private fun drawCircle(map: IMapView) {
        clearMarkers()
        addMarker(
            CircleMapMarker(
                _location ?: map.mapCenter,
                color = _color,
                strokeColor = Color.WHITE,
                strokeWeight = 2f,
                size = 16f
            )
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
        clearMarkers()
        addMarker(
            PathMapMarker(
                _location ?: map.mapCenter,
                path,
                size = 16f,
                color = _color,
                strokeColor = Color.WHITE,
                strokeWeight = 2f,
                rotation = (_azimuth ?: 0f) + map.mapRotation
            )
        )
    }

    protected fun finalize() {
        _path = null
    }
}