package com.kylecorry.trail_sense.shared.camera

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.geometry.Size
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object AugmentedRealityUtils {

    /**
     * Gets the pixel coordinate of a point on the screen given the bearing and azimuth.
     * @param bearing The compass bearing in degrees of the point
     * @param azimuth The compass bearing in degrees that the user is facing (center of the screen)
     * @param altitude The altitude of the point in degrees
     * @param inclination The inclination of the device in degrees
     * @param size The size of the view in pixels
     * @param fov The field of view of the camera in degrees
     */
    fun getPixel(
        bearing: Float,
        azimuth: Float,
        altitude: Float,
        inclination: Float,
        size: Size,
        fov: Size
    ): PixelCoordinate {
        val diagonalFov = hypot(fov.width, fov.height)
        val diagonalSize = hypot(size.width, size.height)
        val radius = diagonalSize / (sin((diagonalFov / 2f).toRadians()) * 2f)

        val newBearing = SolMath.deltaAngle(azimuth, bearing)
        val newAltitude = altitude - inclination

        if (newBearing.absoluteValue > fov.width / 2f || newAltitude.absoluteValue > fov.height / 2f) {
            // TODO: Why is this needed - bearings were looping around when not in place
            // Linear calculation
            val horizontalPixelsPerDegree = size.width / fov.width
            val verticalPixelsPerDegree = size.height / fov.height
            return PixelCoordinate(
                size.width / 2f + newBearing * horizontalPixelsPerDegree,
                size.height / 2f + newAltitude * verticalPixelsPerDegree
            )
        }

        val rectangular = toRectangular(
            newBearing,
            newAltitude,
            radius
        )

        return PixelCoordinate(
            size.width / 2f + rectangular.x,
            size.height / 2f + rectangular.y
        )
    }


    private fun toRectangular(
        bearing: Float,
        altitude: Float,
        radius: Float
    ): PixelCoordinate {
        // X and Y are flipped
        val x = sin(bearing.toRadians()) * cos(altitude.toRadians()) * radius
        val y = cos(bearing.toRadians()) * sin(altitude.toRadians()) * radius
        return PixelCoordinate(x, y)
    }

}

