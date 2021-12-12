package com.kylecorry.trail_sense.tools.clinometer.ui

import com.kylecorry.sol.math.SolMath.tanDegrees
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import kotlin.math.absoluteValue

/**
 * Calculates the location of angles in the image
 * @param focalLength The focal length in millimeters
 * @param sensorSize The sensor size in millimeters
 * @param rotation The sensor rotation in degrees
 */
class ImageAngleCalculator(private val focalLength: Float, private val sensorSize: Size, private val rotation: Int) {

    /**
     * Gets the location of the angles in the image as a percent (0 = top, 1 = bottom, 0 = left, 1 = right)
     * @param horizontalAngle the horizontal angle (deg), where negative is to the left of center and positive to the right
     * @param verticalAngle the vertical angle (deg), where negative is to the bottom of center and positive to the top
     */
    fun getImagePercent(horizontalAngle: Float, verticalAngle: Float): PercentCoordinate {
        val w = if (rotation == 90 || rotation == 270){
            sensorSize.height
        } else {
            sensorSize.width
        }

        val h = if (rotation == 90 || rotation == 270){
            sensorSize.width
        } else {
            sensorSize.height
        }


        val vertical = if (verticalAngle < 0) {
            0.5f + focalLength / h * tanDegrees(verticalAngle.absoluteValue)
        } else {
            0.5f - focalLength / h * tanDegrees(verticalAngle)
        }

        val horizontal = if (horizontalAngle < 0) {
            0.5f - focalLength / w * tanDegrees(horizontalAngle.absoluteValue)
        } else {
            0.5f + focalLength / w * tanDegrees(horizontalAngle)
        }

        return PercentCoordinate(horizontal, vertical)
    }

}