package com.kylecorry.trail_sense.tools.navigation.ui.layers

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.shared.colors.AppColor
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
    private var _cogColor: Int = AppColor.Blue.color
    @ColorInt
    private var _headingColor: Int = AppColor.Red.color
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
        invalidate()
    }

    fun setCOG(cog: Bearing?) {
        _cog = cog
        invalidate()
    }

    fun setUnits(units: DistanceUnits) {
        _units = units
        invalidate()
    }

    fun setBearing(bearing: Float) {
        _heading = bearing
        invalidate()
    }

    fun setSpeed(speed: Speed) {
        _speed = speed
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

    private fun drawCOG(drawer: ICanvasDrawer, map: IMapView, location: Coordinate, predictedLocation: Coordinate, cog: Bearing, @ColorInt color: Int, strokeWeight: Float) {
        val p1 = map.toPixel(location)
        val p2 = map.toPixel(predictedLocation)
        drawer.stroke(color)
        drawer.line(p1.x, p1.y, p2.x, p2.y)
        addMarker(COGArrowMapMarker(predictedLocation ?: return, color, cog = cog.value, strokeWeight = strokeWeight))
    }

    private fun drawHeading(drawer: ICanvasDrawer, map: IMapView, location: Coordinate, headingLocation: Coordinate, heading: Bearing, @ColorInt color: Int, strokeWeight: Float) {
        val p1 = map.toPixel(location)
        val p2 = map.toPixel(headingLocation)
        drawer.stroke(color)
        drawer.line(p1.x, p1.y, p2.x, p2.y)
        addMarker(HeadingArrowMapMarker(headingLocation ?: return, color, heading = heading.value, strokeWeight = strokeWeight))
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        if (!_showCOG) return

        val scale = map.layerScale
        val location = if (isCompass) map.mapCenter else _location
        if (location == null) return // cannot draw anything without a location

        calculateHeadingPosition()
        calculateCOG()

        clearMarkers()

        drawer.noPathEffect()
        drawer.noFill()
        drawer.strokeWeight(6f / scale)

        if (!isCompass && _headingLocation != null && _heading != null) {
            drawHeading(drawer, map, location, _headingLocation!!, Bearing(_heading!!), _headingColor, 6f / scale)
        }
        if (_predictedLocation != null && _cog != null) {
            drawCOG(drawer, map, location, _predictedLocation!!, _cog!!, _cogColor, 6f / scale)
        }

        super.draw(drawer, map)
    }
}