package com.kylecorry.trail_sense.shared.camera

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object AugmentedRealityUtils {

    // TODO: Take in full device orientation / quaternion
    /**
     * Gets the pixel coordinate of a point on the screen given the bearing and azimuth. The point is considered to be on a sphere.
     * @param bearing The compass bearing in degrees of the point
     * @param azimuth The compass bearing in degrees that the user is facing (center of the screen)
     * @param altitude The altitude of the point in degrees
     * @param inclination The inclination of the device in degrees
     * @param size The size of the view in pixels
     * @param fov The field of view of the camera in degrees
     */
    fun getPixelSpherical(
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

        val cartesian = sphericalToCartesian(
            newBearing,
            newAltitude,
            radius
        )

        var x = size.width / 2f + cartesian.x
        // If the coordinate is off the screen, ensure it is not drawn
        if (newBearing > fov.width / 2f){
            x += size.width
        } else if (newBearing < -fov.width / 2f){
            x -= size.width
        }

        var y = size.height / 2f - cartesian.y
        // If the coordinate is off the screen, ensure it is not drawn
        if (newAltitude > fov.height / 2f){
            y += size.height
        } else if (newAltitude < -fov.height / 2f){
            y -= size.height
        }

        return PixelCoordinate(x, y)
    }

    // TODO: Take in full device orientation / quaternion
    /**
     * Gets the pixel coordinate of a point on the screen given the bearing and azimuth. The point is considered to be on a plane.
     * @param bearing The compass bearing in degrees of the point
     * @param azimuth The compass bearing in degrees that the user is facing (center of the screen)
     * @param altitude The altitude of the point in degrees
     * @param inclination The inclination of the device in degrees
     * @param size The size of the view in pixels
     * @param fov The field of view of the camera in degrees
     */
    fun getPixelLinear(
        bearing: Float,
        azimuth: Float,
        altitude: Float,
        inclination: Float,
        size: Size,
        fov: Size
    ): PixelCoordinate {

        val newBearing = SolMath.deltaAngle(azimuth, bearing)
        val newAltitude = altitude - inclination

        val wPixelsPerDegree = size.width / fov.width
        val hPixelsPerDegree = size.height / fov.height

        val x = size.width / 2f + newBearing * wPixelsPerDegree
        val y = size.height / 2f - newAltitude * hPixelsPerDegree

        return PixelCoordinate(x, y)
    }


    /**
     * Converts a spherical coordinate to a cartesian coordinate.
     * @param azimuth The azimuth in degrees (rotation around the z axis)
     * @param elevation The elevation in degrees (rotation around the x axis)
     * @param radius The radius
     */
    private fun sphericalToCartesian(
        azimuth: Float,
        elevation: Float,
        radius: Float
    ): Vector3 {
        // https://stackoverflow.com/questions/5278417/rotating-body-from-spherical-coordinates - this may be useful when factoring in roll
        val azimuthRad = azimuth.toRadians()
        val pitchRad = elevation.toRadians()

        val sinAzimuth = sin(azimuthRad)

        val x = sinAzimuth * cos(pitchRad) * radius
        val y = sinAzimuth * sin(pitchRad) * radius
        val z = cos(azimuthRad) * radius

        return Vector3(x, y, z)
    }

}

