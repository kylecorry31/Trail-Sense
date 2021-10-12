package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.geometry.Circle
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.maps.ICoordinateToPixelStrategy

class RadarCompassCoordinateToPixelStrategy(
    private val radar: Circle,
    private val area: Geofence,
    private val useTrueNorth: Boolean,
    private val declination: Float
) : ICoordinateToPixelStrategy {

    private val metersPerPixel = area.radius.meters().distance / radar.radius

    override fun getPixels(coordinate: Coordinate): PixelCoordinate {
        val distance = area.center.distanceTo(coordinate)
        val bearing = if (useTrueNorth) {
            area.center.bearingTo(coordinate)
        } else {
            DeclinationUtils.fromTrueNorthBearing(area.center.bearingTo(coordinate), declination)
        }
        val angle = SolMath.wrap(-(bearing.value - 90), 0f, 360f)
        val pixelDistance = distance / metersPerPixel
        val xDiff = SolMath.cosDegrees(angle.toDouble()).toFloat() * pixelDistance
        val yDiff = SolMath.sinDegrees(angle.toDouble()).toFloat() * pixelDistance
        return PixelCoordinate(radar.center.x + xDiff, radar.center.y - yDiff)
    }

}