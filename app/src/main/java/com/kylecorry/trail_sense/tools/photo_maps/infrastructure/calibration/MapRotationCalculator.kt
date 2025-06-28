package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.calibration

import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.analysis.Trigonometry
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.toVector2
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapProjectionFactory
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
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
    fun calculate(map: PhotoMap): Float {
        if (!map.isCalibrated){
            return 0f
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
                return 180f
            }

            return 0f
        }


        val size = map.metadata.size
        val pixels = map.calibration.calibrationPoints.map {
            it.imageLocation.toPixels(
                size.width,
                size.height
            ).toVector2(size.height)
        }

        val baseProjection = MapProjectionFactory().getProjection(map.metadata.projection)

        val projectedPixels = map.calibration.calibrationPoints.map {
            baseProjection.toPixels(it.location)
        }

        val pixelAngle = Trigonometry.remapUnitAngle(
            pixels[0].angleBetween(pixels[1]),
            90f,
            false
        )

        val locationAngle = Trigonometry.remapUnitAngle(
            projectedPixels[0].angleBetween(projectedPixels[1]),
            90f,
            false
        )

        return SolMath.normalizeAngle(SolMath.deltaAngle(pixelAngle, locationAngle))
    }

}