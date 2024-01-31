package com.kylecorry.trail_sense.tools.augmented_reality.domain.mapper

import android.graphics.RectF
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

interface CameraAnglePixelMapper {
    /**
     * Get the real world angles of the pixel.
     * @param x The x pixel coordinate
     * @param y The y pixel coordinate
     * @param imageRect The image rect
     * @param fieldOfView The field of view of the camera
     * @return The angle (negative is left or below center)
     */
    fun getAngle(
        x: Float,
        y: Float,
        imageRect: RectF,
        fieldOfView: Size
    ): Vector2

    /**
     * Get the pixel coordinate of the real world angle.
     * @param angleX The horizontal angle (negative is left of center)
     * @param angleY The vertical angle (negative is below center)
     * @param imageRect The image rect
     * @param fieldOfView The field of view of the camera
     * @param distance The distance to the object in meters (optional)
     * @return The pixel coordinate
     */
    fun getPixel(
        angleX: Float,
        angleY: Float,
        imageRect: RectF,
        fieldOfView: Size,
        distance: Float? = null
    ): PixelCoordinate

    /**
     * Get the pixel coordinate of the real world point.
     * @param world The point in the world in the camera's coordinate system (z is forward, y is up, x is right)
     * @param imageRect The image rect
     * @param fieldOfView The field of view of the camera
     * @return The pixel coordinate
     */
    fun getPixel(
        world: Vector3,
        imageRect: RectF,
        fieldOfView: Size,
    ): PixelCoordinate

    companion object {
        /**
         * Converts a spherical coordinate in the camera space to a cartesian coordinate in the camera space.
         * @param bearing The bearing in degrees (positive is right of center)
         * @param altitude The altitude in degrees (positive is above center)
         * @param radius The radius
         * @return The cartesian coordinate
         */
        fun toCartesian(
            bearing: Float,
            altitude: Float,
            radius: Float
        ): Vector3 {
            val altitudeRad = altitude.toRadians()
            val bearingRad = bearing.toRadians()
            val cosAltitude = cos(altitudeRad)
            val sinAltitude = sin(altitudeRad)
            val cosBearing = cos(bearingRad)
            val sinBearing = sin(bearingRad)

            // X and Y are flipped
            val x = sinBearing * cosAltitude * radius
            val y = cosBearing * sinAltitude * radius
            val z = cosBearing * cosAltitude * radius
            return Vector3(x, y, z)
        }

        /**
         * Converts a cartesian coordinate in the camera space to a spherical coordinate in the camera space.
         * @param vector The cartesian coordinate
         * @return The spherical coordinate (radius, altitude, bearing)
         */
        fun toSpherical(vector: Vector3): Vector3 {
            val r = vector.magnitude()
            val theta = asin(vector.y / r).toDegrees().real(0f)
            val phi = atan2(vector.x, vector.z).toDegrees().real(0f)
            return Vector3(r, theta, phi)
        }
    }
}