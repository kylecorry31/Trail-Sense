package com.kylecorry.trail_sense.tools.maps.infrastructure.calibration

import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.roundNearest
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.toVector2
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import kotlin.math.absoluteValue
import kotlin.math.atan2

class MapRotationCalculator {

    fun calculate(map: PhotoMap): Int {
        // If the map is large, only allow it to be flipped vertically
        val bounds = CoordinateBounds.from(map.calibration.calibrationPoints.map { it.location })
        val east = bounds.east
        val west = bounds.west
        if ((east - west).absoluteValue > 180){
            // Min and max are flipped because it is dealing with image coordinates from the top left
            val top = map.calibration.calibrationPoints.minBy { it.imageLocation.y }.location
            val bottom = map.calibration.calibrationPoints.maxBy { it.imageLocation.y }.location

            if (top.latitude < bottom.latitude){
                return 180
            }

            return 0
        }


        val size = map.metadata.size
        val pixels = map.calibration.calibrationPoints.map {
            it.imageLocation.toPixels(
                size.width,
                size.height
            ).toVector2(size.height)
        }
        val locations = map.calibration.calibrationPoints.map { it.location }

        val pixelAngle = SolMath.normalizeAngle(
            -atan2(
                pixels[1].y - pixels[0].y,
                pixels[1].x - pixels[0].x
            ).toDegrees()
        ) + 90f

        val bearing = locations[0].bearingTo(locations[1])

        // TODO: Once infrastructure is ready, remove the rounding
        return SolMath.deltaAngle(pixelAngle, bearing.value).roundNearest(90f).toInt()
    }

}