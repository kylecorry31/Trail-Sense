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

    private val lastFovLock = Any()
    private var lastFov: Size? = null
    private var lastFocalLength = 0f

    // Constants for perspective projection
    private const val minDistance = 0.1f
    private const val maxDistance = 1000f
    private const val rangeReciprocal = 1 / (minDistance - maxDistance)
    private const val zMultiplier = (maxDistance + minDistance) * rangeReciprocal
    private const val zOffset = 2 * maxDistance * minDistance * rangeReciprocal

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
        return AugmentedRealityView.HorizonCoordinate(bearing, elevationAngle, distance, true)
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
        distance: Float,
        rotationMatrix: FloatArray,
        size: Size,
        fov: Size
    ): PixelCoordinate {

        val d = distance.coerceIn(minDistance, maxDistance)

        val spherical = toRelative(bearing, elevation, d, rotationMatrix)
        // The rotation of the device has been negated, so azimuth = 0 and inclination = 0 is used
        return getPixelPerspective(spherical.first, 0f, spherical.second, 0f, d, size, fov)
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
        distance: Float,
        size: Size,
        fov: Size
    ): PixelCoordinate {
        // This doesn't work well for large fovs, so fall back to linear
        if (fov.width > 65) {
            return getPixelLinear(bearing, azimuth, altitude, inclination, size, fov)
        }

        val newBearing = SolMath.deltaAngle(azimuth, bearing)
        val newAltitude = altitude - inclination
        val world = toRectangular(newBearing, newAltitude, distance)

        // Point is behind the camera, so calculate the linear projection
        if (world.z < 0) {
            return getPixelLinear(bearing, azimuth, altitude, inclination, size, fov)
        }

        // Perspective matrix multiplication - written out to avoid unnecessary allocations and calculations
        val f = synchronized(lastFovLock) {
            if (lastFov == fov) {
                lastFocalLength
            } else {
                val focalLength = 1 / SolMath.tanDegrees(fov.height / 2)
                lastFov = fov
                lastFocalLength = focalLength
                focalLength
            }
        }

        val aspect = fov.width / fov.height
        val x = f / aspect * world.x
        val y = f * world.y
        var z = zMultiplier * world.z + zOffset
        if (z == 0f) {
            // Prevent NaN
            z = 1f
        }

        val screenX = x / z
        val screenY = y / z

        val pixelX = (1 - screenX) / 2f * size.width
        val pixelY = (screenY + 1) / 2f * size.height

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

