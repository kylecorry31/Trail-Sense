package com.kylecorry.trail_sense.navigation.domain

import android.graphics.Path
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils

class RenderedPathFactory(
    private val metersPerPixel: Float,
    private val origin: Coordinate?,
    private val declination: Float,
    private val useTrueNorth: Boolean
) {
    fun render(points: List<Coordinate>, path: Path = Path()): RenderedPath {
        val origin = origin ?: CoordinateBounds.from(points).center
        for (i in 1 until points.size) {
            if (i == 1) {
                val start = toPixels(points[0], origin)
                path.moveTo(start.x, start.y)
            }

            val end = toPixels(points[i], origin)
            path.lineTo(end.x, end.y)
        }
        return RenderedPath(origin, path)
    }

    private fun toPixels(coordinate: Coordinate, origin: Coordinate): PixelCoordinate {
        val distance = origin.distanceTo(coordinate)
        val direction = if (useTrueNorth){
            origin.bearingTo(coordinate)
        } else {
            DeclinationUtils.fromTrueNorthBearing(origin.bearingTo(coordinate), declination)
        }
        val angle = SolMath.wrap(-(direction.value - 90), 0f, 360f)
        val pixelDistance = distance / metersPerPixel
        val xDiff = SolMath.cosDegrees(angle.toDouble()).toFloat() * pixelDistance
        val yDiff = SolMath.sinDegrees(angle.toDouble()).toFloat() * pixelDistance
        return PixelCoordinate(xDiff, -yDiff)
    }

}