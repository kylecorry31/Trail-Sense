package com.kylecorry.trail_sense.navigation.domain

import android.graphics.Path
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils

class RenderedPathFactory(
    private val metersPerPixel: Float,
    private val declination: Float,
    private val useTrueNorth: Boolean
) {
    fun createPath(points: List<Coordinate>, path: Path = Path()): RenderedPath {
        val center = CoordinateBounds.from(points).center
        for (i in 1 until points.size) {
            if (i == 1) {
                val start = toPixels(points[0], center)
                path.moveTo(start.x, start.y)
            }

            val end = toPixels(points[i], center)
            path.lineTo(end.x, end.y)
        }
        return RenderedPath(center, path)
    }

    private fun toPixels(coordinate: Coordinate, center: Coordinate): PixelCoordinate {
        val distance = center.distanceTo(coordinate)
        val direction = if (useTrueNorth){
            center.bearingTo(coordinate)
        } else {
            DeclinationUtils.fromTrueNorthBearing(center.bearingTo(coordinate), declination)
        }
        val angle = SolMath.wrap(-(direction.value - 90), 0f, 360f)
        val pixelDistance = distance / metersPerPixel
        val xDiff = SolMath.cosDegrees(angle.toDouble()).toFloat() * pixelDistance
        val yDiff = SolMath.sinDegrees(angle.toDouble()).toFloat() * pixelDistance
        return PixelCoordinate(xDiff, -yDiff)
    }

}