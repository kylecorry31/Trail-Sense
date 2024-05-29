package com.kylecorry.trail_sense.tools.navigation.ui.layers

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.tools.navigation.domain.LocationMath
import com.kylecorry.trail_sense.tools.navigation.ui.markers.COGArrowMapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.HeadingArrowMapMarker

class CourseLayer(val isCompass: Boolean = false) : BaseLayer() {

    private var _location: Coordinate? = null
    private var _predictedLocation: Coordinate? = null
    private var _headingLocation: Coordinate? = null
    private var _cog: Bearing? = null
    private var _heading: Float? = null
    private var _speed: Speed? = null
    private var _showCOG: Boolean = true
    @ColorInt
    private var _cogColor: Int? = null
    @ColorInt
    private var _headingColor: Int? = null
    private var _units: DistanceUnits? = null

    fun setShowCOG(show: Boolean) {
        _showCOG = show
        invalidate()
    }

    private fun calculateCOG() {
        val location = _location ?: return
        val speed = _speed ?: return
        val distance = Distance(LocationMath.convertUnitPerSecondsToUnitPerHours(speed.speed), speed.distanceUnits)
        val cog = _cog ?: return
        _predictedLocation = location.plus(distance, cog)
    }

    private fun calculateHeadingPosition() {
        val location = _location ?: return
        val distance = Distance(1f, _units ?: return)
        val heading = _heading ?: return
        _headingLocation = location.plus(distance, Bearing(heading))
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

    fun setUnits(units: DistanceUnits) {
        _units = units
        invalidate()
    }

    fun setBearing(bearing: Float) {
        _heading = bearing
        calculateHeadingPosition()
        invalidate()
    }

    fun setSpeed(speed: Speed) {
        _speed = speed
        calculateCOG()
        invalidate()
    }

    fun setCOGColor(@ColorInt color: Int) {
        _cogColor = color
        invalidate()
    }

    fun setHeadingColor(@ColorInt color: Int) {
        _headingColor = color
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        if (!_showCOG) return

        val scale = map.layerScale
        val tmp = if (isCompass) map.mapCenter else _location
        val p1 = tmp?.let { map.toPixel(it) } ?: return
        var p2 = _headingLocation?.let { map.toPixel(it) } ?: return
        drawer.noPathEffect()
        drawer.noFill()
        drawer.strokeWeight(6f / scale)
        if (!isCompass) {
            drawer.stroke(_headingColor ?: return)
            drawer.line(p1.x, p1.y, p2.x, p2.y)
        }

        p2 = _predictedLocation?.let { map.toPixel(it) } ?: return
        drawer.stroke(_cogColor ?: return)
        drawer.line(p1.x, p1.y, p2.x, p2.y)

        clearMarkers()
        if (!isCompass) {
            addMarker(HeadingArrowMapMarker(_headingLocation ?: return, _headingColor ?: return, heading = (_heading ?: return), strokeWeight = 6f / scale))
        }
        addMarker(COGArrowMapMarker(_predictedLocation ?: return, _cogColor ?: return, cog = (_cog ?: return).value, strokeWeight = 6f / scale))
        super.draw(drawer, map)
    }
}