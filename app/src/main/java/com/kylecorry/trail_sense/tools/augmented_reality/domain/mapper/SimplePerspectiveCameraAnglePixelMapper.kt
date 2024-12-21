package com.kylecorry.trail_sense.tools.augmented_reality.domain.mapper

import android.graphics.RectF
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.optics.Optics
import kotlin.math.cos
import kotlin.math.sin

/**
 * A camera angle pixel mapper that uses a perspective projection to map angles to pixels.
 * This mapper does not use near and far clipping planes.
 */
class SimplePerspectiveCameraAnglePixelMapper : CameraAnglePixelMapper {

    private val linear = LinearCameraAnglePixelMapper()

    override fun getAngle(
        x: Float,
        y: Float,
        imageRect: RectF,
        fieldOfView: Size
    ): Vector2 {
        val fx = Optics.getFocalLength(fieldOfView.width, imageRect.width())
        val fy = Optics.getFocalLength(fieldOfView.height, imageRect.height())

        val worldZ = 1f
        val worldX = (x - imageRect.centerX()) * worldZ / fx
        val worldY = -(y - imageRect.centerY()) * worldZ / fy

        val world = Vector3(worldX, worldY, worldZ)
        val spherical = CameraAnglePixelMapper.toSpherical(world)
        return Vector2(spherical.z, spherical.y)
    }

    override fun getPixel(
        angleX: Float,
        angleY: Float,
        imageRect: RectF,
        fieldOfView: Size,
        distance: Float?
    ): PixelCoordinate {
        val world = toCartesian(angleX, angleY, distance ?: 1f)
        return getPixel(world, imageRect, fieldOfView)
    }

    override fun getPixel(world: Vector3, imageRect: RectF, fieldOfView: Size): PixelCoordinate {
        // Point is behind the camera, so calculate the linear projection
        if (world.z < 0) {
            return linear.getPixel(world, imageRect, fieldOfView)
        }

        val fy = Optics.getFocalLength(fieldOfView.height, imageRect.height())
        val fx = Optics.getFocalLength(fieldOfView.width, imageRect.width())

        val x = fx * world.x
        val y = fy * world.y

        val screenX = x / world.z + imageRect.centerX()
        val screenY = -y / world.z + imageRect.centerY()

        return PixelCoordinate(screenX, screenY)
    }

    private fun toCartesian(
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
}