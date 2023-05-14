package com.kylecorry.trail_sense.tools.maps.infrastructure.calibration

import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.roundNearestAngle
import com.kylecorry.sol.math.analysis.Trigonometry
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.toVector2
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import kotlin.math.absoluteValue

/**
 * A class that calculates the rotation of a map based on the calibration points
 */
class MapRotationCalculator {

    /**
     * Calculates the ideal rotation of the map
     * @param map: The map to calculate the rotation for
     * @return The rotation in degrees
     */
    fun calculate(map: PhotoMap): Int {
        if (!map.isCalibrated){
            return 0
        }


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

        val pixelAngle = Trigonometry.remapUnitAngle(
            pixels[0].angleBetween(pixels[1]),
            90f,
            false
        )

        val bearing = locations[0].bearingTo(locations[1])

        // TODO: Once infrastructure is ready, remove the rounding
        return SolMath.deltaAngle(pixelAngle, bearing.value).roundNearestAngle(90f).toInt()
    }

}