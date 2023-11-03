package com.kylecorry.trail_sense.shared.camera

import android.hardware.SensorManager
import android.opengl.Matrix
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.sol.math.QuaternionMath
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.augmented_reality.AugmentedRealityView
import kotlin.math.atan2
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
        if (newBearing > fov.width / 2f) {
            x += size.width
        } else if (newBearing < -fov.width / 2f) {
            x -= size.width
        }

        var y = size.height / 2f - cartesian.y
        // If the coordinate is off the screen, ensure it is not drawn
        if (newAltitude > fov.height / 2f) {
            y += size.height
        } else if (newAltitude < -fov.height / 2f) {
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

    fun getAngularSize(diameter: Distance, distance: Distance): Float {
        return getAngularSize(diameter.meters().distance, distance.meters().distance)
    }

    fun getAngularSize(diameterMeters: Float, distanceMeters: Float): Float {
        return (2 * atan2(diameterMeters / 2f, distanceMeters)).toDegrees()
    }

    fun getHorizonCoordinate(
        myLocation: Coordinate,
        myElevation: Float,
        destinationCoordinate: Coordinate,
        destinationElevation: Float? = null
    ): AugmentedRealityView.HorizonCoordinate {
        val bearing = myLocation.bearingTo(destinationCoordinate).value
        val distance = myLocation.distanceTo(destinationCoordinate)
        val elevationAngle = if (destinationElevation == null) {
            0f
        } else {
            atan2((destinationElevation - myElevation), distance).toDegrees()
        }
        return AugmentedRealityView.HorizonCoordinate(bearing, elevationAngle, true)
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


    /**
     * Computes the orientation of the device in the AR coordinate system.
     * @param orientationSensor The orientation sensor
     * @param quaternion The array to store the quaternion in
     * @param rotationMatrix the array to store the rotation matrix in
     * @param orientation The array to store the orientation in (azimuth, pitch, roll in degrees)
     * @param declination The declination to use (default null)
     */
    fun getOrientation(
        orientationSensor: IOrientationSensor,
        quaternion: FloatArray,
        rotationMatrix: FloatArray,
        orientation: FloatArray,
        declination: Float? = null
    ) {
        // Convert the orientation a rotation matrix
        QuaternionMath.inverse(orientationSensor.rawOrientation, quaternion)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, quaternion)

        // Remap the coordinate system to AR space
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            rotationMatrix
        )

        // Add declination
        if (declination != null) {
            Matrix.rotateM(rotationMatrix, 0, declination, 0f, 0f, 1f)
        }

        // Get orientation from rotation matrix
        SensorManager.getOrientation(rotationMatrix, orientation)
        orientation[0] = orientation[0].toDegrees()
        orientation[1] = -orientation[1].toDegrees()
        orientation[2] = -orientation[2].toDegrees()
    }

}

