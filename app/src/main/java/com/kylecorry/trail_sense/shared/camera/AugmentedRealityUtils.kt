package com.kylecorry.trail_sense.shared.camera

import android.graphics.RectF
import android.opengl.Matrix
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.andromeda.sense.orientation.OrientationUtils
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.augmented_reality.domain.mapper.CameraAnglePixelMapper
import com.kylecorry.trail_sense.tools.augmented_reality.domain.mapper.LinearCameraAnglePixelMapper
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.SphericalARPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object AugmentedRealityUtils {

    private val worldVectorLock = Any()
    private val tempWorldVector = FloatArray(4)
    private val tempRotationMatrix = FloatArray(16)

    // Constants for perspective projection
    private const val minDistance = 0.1f
    private const val maxDistance = 1000f

    private val linear = LinearCameraAnglePixelMapper()
    private val rect = RectF()
    private val rectLock = Any()

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

        return synchronized(rectLock) {
            rect.right = size.width
            rect.bottom = size.height
            linear.getPixel(
                newBearing,
                newAltitude,
                rect,
                fov,
                null
            )
        }
    }

    fun getAngularSize(diameter: Distance, distance: Distance): Float {
        return getAngularSize(diameter.meters().distance, distance.meters().distance)
    }

    fun getAngularSize(diameterMeters: Float, distanceMeters: Float): Float {
        return (2 * atan2(diameterMeters / 2f, distanceMeters)).toDegrees()
    }

    /**
     * Gets the pixel coordinate of a point on the screen given the bearing and azimuth.
     * @param bearing The compass bearing in degrees of the point
     * @param elevation The elevation in degrees of the point
     * @param rotationMatrix The rotation matrix of the device in the AR coordinate system
     * @param rect The rectangle of the view in pixels
     * @param fov The field of view of the camera in degrees
     * @param mapper A mapper to use
     */
    fun getPixel(
        bearing: Float,
        elevation: Float,
        distance: Float,
        rotationMatrix: FloatArray,
        rect: RectF,
        fov: Size,
        mapper: CameraAnglePixelMapper
    ): PixelCoordinate {
        val d = distance.coerceIn(minDistance, maxDistance)

        return getPixel(
            toEastNorthUp(bearing, elevation, d),
            rotationMatrix,
            rect,
            fov,
            mapper
        )
    }

    /**
     * Gets the pixel coordinate of a point in the ENU coordinate system.
     * @param enuCoordinate The ENU coordinate of the point
     * @param rotationMatrix The rotation matrix of the device in the AR coordinate system
     * @param rect The rectangle of the view in pixels
     * @param fov The field of view of the camera in degrees
     * @param mapper A mapper to use
     * @return The pixel coordinate
     */
    fun getPixel(
        enuCoordinate: Vector3,
        rotationMatrix: FloatArray,
        rect: RectF,
        fov: Size,
        mapper: CameraAnglePixelMapper
    ): PixelCoordinate {
        val world = enuToAr(enuCoordinate, rotationMatrix)

        return mapper.getPixel(
            world,
            rect,
            fov
        )
    }

    fun getCoordinate(
        pixel: PixelCoordinate,
        rotationMatrix: FloatArray,
        rect: RectF,
        fov: Size,
        mapper: CameraAnglePixelMapper
    ): Vector3 {
        val world = mapper.getAngle(pixel.x, pixel.y, rect, fov)
        // TODO: Get this working for all mappers
//        val inversePerspective = Optics.inversePerspectiveProjection(
////            Vector2(pixel.x, pixel.y),
//            pixel.toVector2(rect.top),
//            Vector2(
//                Optics.getFocalLength(fov.width, rect.width()),
//                Optics.getFocalLength(fov.height, rect.height())
//            ),
//            PixelCoordinate(rect.centerX(), rect.centerY()).toVector2(rect.top),
//            100f
//        )
        val spherical = SphericalARPoint(world.x, world.y).coordinate.position
        return arToEnu(spherical, rotationMatrix)
    }


    /**
     * Computes the orientation of the device in the AR coordinate system.
     * @param orientationSensor The orientation sensor
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

    fun toEastNorthUp(
        myLocation: Coordinate,
        myElevation: Float,
        destinationCoordinate: Coordinate,
        destinationElevation: Float
    ): Vector3 {
        // TODO: Go directly to ENU
        val bearing = myLocation.bearingTo(destinationCoordinate).value
        val distance = myLocation.distanceTo(destinationCoordinate)
        val elevationAngle = if (abs(myElevation - destinationElevation) < 0.0001f) {
            0f
        } else {
            atan2((destinationElevation - myElevation), distance).toDegrees()
        }
        return toEastNorthUp(bearing, elevationAngle, distance)
    }

    /**
     * Converts a spherical coordinate to a cartesian coordinate in the East-North-Up (ENU) coordinate system.
     * @param bearing The azimuth in degrees (rotation around the z axis)
     * @param elevation The elevation in degrees (rotation around the x axis)
     * @param distance The distance in meters
     */
    fun toEastNorthUp(
        bearing: Float,
        elevation: Float,
        distance: Float
    ): Vector3 {
        val d = distance.coerceIn(minDistance, maxDistance)

        val elevationRad = elevation.toRadians()
        val bearingRad = bearing.toRadians()

        val cosElevation = cos(elevationRad)
        val x = d * cosElevation * sin(bearingRad) // East
        val y = d * cosElevation * cos(bearingRad) // North
        val z = d * sin(elevationRad) // Up
        return Vector3(x, y, z)
    }

    /**
     * Converts a cartesian coordinate in the East-North-Up (ENU) coordinate system to a cartesian coordinate in the AR coordinate system.
     * @param enu The ENU coordinate
     * @param rotationMatrix The rotation matrix of the device in the AR coordinate system
     * @return The AR coordinate
     */
    fun enuToAr(enu: Vector3, rotationMatrix: FloatArray): Vector3 {
        return synchronized(worldVectorLock) {
            tempWorldVector[0] = enu.x
            tempWorldVector[1] = enu.y
            tempWorldVector[2] = enu.z
            tempWorldVector[3] = 1f
            Matrix.multiplyMV(tempWorldVector, 0, rotationMatrix, 0, tempWorldVector, 0)
            // Swap y and z to convert to AR coordinate system
            Vector3(tempWorldVector[0], tempWorldVector[2], tempWorldVector[1])
        }
    }

    fun arToEnu(ar: Vector3, rotationMatrix: FloatArray): Vector3 {
        return synchronized(worldVectorLock) {
            tempWorldVector[0] = ar.x
            tempWorldVector[1] = ar.y
            tempWorldVector[2] = ar.z
            tempWorldVector[3] = 1f

            // Invert the rotation matrix
            Matrix.invertM(tempRotationMatrix, 0, rotationMatrix, 0)

            Matrix.multiplyMV(tempWorldVector, 0, tempRotationMatrix, 0, tempWorldVector, 0)
            Vector3(tempWorldVector[0], tempWorldVector[1], tempWorldVector[2])
        }
    }

}

