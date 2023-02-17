package com.kylecorry.trail_sense.tools.maps.ui

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.ui.layers.BaseLayer
import com.kylecorry.trail_sense.navigation.ui.layers.IMapView
import com.kylecorry.trail_sense.navigation.ui.markers.CircleMapMarker

/**
 * Draws a circle on the map representing the accuracy of a location
 */
class MyAccuracyLayer : BaseLayer() {

    /**
     * The location where the circle will be placed
     */
    private var _location: Coordinate? = null

    /**
     * The radius of the circle in meters
     */
    private var _accuracy: Float? = null

    /**
     * The fill color of the circle
     */
    private var _fillColor: Int = Color.WHITE

    /**
     * The stroke color of the circle
     */
    private var _strokeColor: Int = Color.WHITE

    /**
     * Sets the location and accuracy of the circle
     * @param location The location of the circle
     * @param accuracy The radius of the circle in meters
     */
    fun setParameters(location: Coordinate?, accuracy: Float?) {
        _location = location
        _accuracy = accuracy
        invalidate()
    }

    /**
     * Sets the fill and stroke color of the circle
     * @param fillColor The fill color of the circle
     * @param strokeColor The stroke color of the circle
     */
    fun setColors(@ColorInt fillColor: Int, @ColorInt strokeColor: Int) {
        _fillColor = fillColor
        _strokeColor = strokeColor
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        clearMarkers()
        if (_accuracy == null || _location == null) return

        addMarker(
            CircleMapMarker(
                _location!!,
                _fillColor,
                _strokeColor,
                128,
                (_accuracy!!) / map.metersPerPixel
            )
        )
        super.draw(drawer, map)
    }
}