package com.kylecorry.trail_sense.shared.camera

import android.opengl.Matrix
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.andromeda.sense.orientation.OrientationUtils
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.augmented_reality.AugmentedRealityView
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object AugmentedRealityUtils {

    private val worldVectorLock = Any()
    private val tempWorldVector = FloatArray(4)

    private val perspectiveLock = Any()
    private val tempPerspective = FloatArray(16)

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
     * Gets the pixel coordinate of a point on the screen given the bearing and azimuth.
     * @param bearing The compass bearing in degrees of the point
     * @param elevation The elevation in degrees of the point
     * @param rotationMatrix The rotation matrix of the device in the AR coordinate system
     * @param size The size of the view in pixels
     * @param fov The field of view of the camera in degrees
     */
    fun getPixel(
        bearing: Float,
        elevation: Float,
        rotationMatrix: FloatArray,
        size: Size,
        fov: Size
    ): PixelCoordinate {
        val spherical = toRelative(bearing, elevation, 1f, rotationMatrix)
        // The rotation of the device has been negated, so azimuth = 0 and inclination = 0 is used
        return getPixelPerspective(spherical.first, 0f, spherical.second, 0f, size, fov)
    }

    /**
     * Gets the pixel coordinate of a point on the screen given the bearing and azimuth. The point is considered to be on a plane.
     * @param bearing The compass bearing in degrees of the point
     * @param azimuth The compass bearing in degrees that the user is facing (center of the screen)
     * @param altitude The altitude of the point in degrees
     * @param inclination The inclination of the device in degrees
     * @param size The size of the view in pixels
     * @param fov The field of view of the camera in degrees
     */
    fun getPixelPerspective(
        bearing: Float,
        azimuth: Float,
        altitude: Float,
        inclination: Float,
        size: Size,
        fov: Size
    ): PixelCoordinate {

        val linear = getPixelLinear(bearing, azimuth, altitude, inclination, size, fov)

        if (fov.width > 65){
            return linear
        }

        val newBearing = SolMath.deltaAngle(azimuth, bearing)
        val newAltitude = altitude - inclination
        val world = toRectangular(newBearing, newAltitude, 1f)
        
        synchronized(perspectiveLock) {
            Matrix.perspectiveM(
                tempPerspective,
                0,
                fov.height,
                fov.width / fov.height,
                0.1f,
                100f
            )

            // Map the world coordinate to the screen using the perspective matrix
            synchronized(worldVectorLock) {
                tempWorldVector[0] = world.x
                tempWorldVector[1] = world.y
                tempWorldVector[2] = world.z
                tempWorldVector[3] = 1f
                Matrix.multiplyMV(tempWorldVector, 0, tempPerspective, 0, tempWorldVector, 0)
            }
        }

        // Get the screen coordinate
        val screenX = tempWorldVector[0] / tempWorldVector[3]
        val screenY = tempWorldVector[1] / tempWorldVector[3]

        val pixelX = if (linear.x in (-size.width / 2f)..(size.width + size.width / 2f)) {
            (1 - screenX) / 2f * size.width
        } else {
            linear.x
        }
        val pixelY = if (linear.y in (-size.height / 2f)..(size.height + size.height / 2f)) {
            (1 - screenY) / 2f * size.height
            (screenY + 1) / 2f * size.height
        } else {
            linear.y
        }

        return PixelCoordinate(pixelX, pixelY)

    }

    private fun toRectangular(
        bearing: Float,
        altitude: Float,
        radius: Float
    ): Vector3 {
        // X and Y are flipped
        val x = sin(bearing.toRadians()) * cos(altitude.toRadians()) * radius
        val y = cos(bearing.toRadians()) * sin(altitude.toRadians()) * radius
        val z = cos(bearing.toRadians()) * cos(altitude.toRadians()) * radius
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
        rotationMatrix: FloatArray,
        orientation: FloatArray,
        declination: Float? = null
    ) {
        OrientationUtils.getAROrientation(
            orientationSensor,
            rotationMatrix,
            orientation,
            declination
        )
    }

    /**
     * Converts a spherical coordinate to a cartesian coordinate in the AR coordinate system.
     * @param bearing The azimuth in degrees (rotation around the z axis)
     * @param elevation The elevation in degrees (rotation around the x axis)
     * @param distance The distance in meters
     */
    private fun toWorldCoordinate(
        bearing: Float,
        elevation: Float,
        distance: Float
    ): Vector3 {
        val thetaRad = elevation.toRadians()
        val phiRad = bearing.toRadians()

        val cosTheta = cos(thetaRad)
        val x = distance * cosTheta * sin(phiRad)
        val y = distance * cosTheta * cos(phiRad)
        val z = distance * sin(thetaRad)
        return Vector3(x, y, z)
    }

    private fun toSpherical(vector: Vector3): Vector3 {
        val r = vector.magnitude()
        val theta = asin(vector.z / r).toDegrees().real(0f)
        val phi = atan2(vector.x, vector.y).toDegrees().real(0f)
        return Vector3(r, theta, phi)
    }

    /**
     * Converts a geographic spherical coordinate to a relative spherical coordinate in the AR coordinate system.
     * @return The relative spherical coordinate (bearing, inclination)
     */
    private fun toRelative(
        bearing: Float,
        elevation: Float,
        distance: Float,
        rotationMatrix: FloatArray
    ): Pair<Float, Float> {
        // Convert to world space
        val worldVector = toWorldCoordinate(bearing, elevation, distance)

        // Rotate
        val rotated = synchronized(worldVectorLock) {
            tempWorldVector[0] = worldVector.x
            tempWorldVector[1] = worldVector.y
            tempWorldVector[2] = worldVector.z
            tempWorldVector[3] = 1f
            Matrix.multiplyMV(tempWorldVector, 0, rotationMatrix, 0, tempWorldVector, 0)
            Vector3(tempWorldVector[0], tempWorldVector[1], tempWorldVector[2])
        }

        // Convert back to spherical
        val spherical = toSpherical(rotated)
        return spherical.z to spherical.y
    }

}

