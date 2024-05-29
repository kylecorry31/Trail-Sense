package com.kylecorry.trail_sense.tools.navigation.ui.layers

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.tools.navigation.domain.LocationMath
import com.kylecorry.trail_sense.tools.navigation.ui.markers.COGArrowMapMarker

class COGLayer(val isCompass: Boolean = false) : BaseLayer() {

    private var _location: Coordinate? = null
    private var _predictedLocation: Coordinate? = null
    private var _cog: Bearing? = null
    private var _speed: Speed? = null
    private var _showCOG: Boolean = true
    @ColorInt
    private var _color: Int? = null

    fun setShowCOG(show: Boolean) {
        _showCOG = show
        invalidate()
    }

    fun calculateCOG() {
        val location = _location ?: return
        val speed = _speed ?: return
        val distance = Distance(LocationMath.convertUnitPerSecondsToUnitPerHours(speed.speed), speed.distanceUnits)
        val cog = _cog ?: return
        _predictedLocation = location.plus(distance, cog)
    }

    fun setLocation(location: Coordinate) {
        _location = location
        calculateCOG()
        invalidate()
    }

    fun setCOG(cog: Bearing?) {
        _cog = cog
        calculateCOG()
        invalidate()
    }

    fun setSpeed(speed: Speed) {
        _speed = speed
        calculateCOG()
        invalidate()
    }

    fun setColor(@ColorInt color: Int) {
        _color = color
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        if (!_showCOG) return

        val scale = map.layerScale
        val tmp = if (isCompass) map.mapCenter else _location
        val p1 = tmp?.let { map.toPixel(it) } ?: return
        val p2 = _predictedLocation?.let { map.toPixel(it) } ?: return
        drawer.noPathEffect()
        drawer.noFill()
        drawer.stroke(_color ?: return)
        drawer.strokeWeight(6f / scale)
        drawer.line(p1.x, p1.y, p2.x, p2.y)

        clearMarkers()
        addMarker(COGArrowMapMarker(_predictedLocation ?: return, _color ?: return, cog = (_cog ?: return).value, strokeWeight = 6f / scale))
        super.draw(drawer, map)
    }
}