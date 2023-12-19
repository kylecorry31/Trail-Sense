package com.kylecorry.trail_sense.shared.views.camera

import android.graphics.RectF
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.shared.views.camera.CameraAnglePixelMapper
import com.kylecorry.trail_sense.shared.views.camera.LinearCameraAnglePixelMapper
import kotlin.math.cos
import kotlin.math.sin

class PerspectiveCameraAnglePixelMapper(
    private val nearDistance: Float,
    private val farDistance: Float
) : CameraAnglePixelMapper {

    private val rangeReciprocal = 1 / (nearDistance - farDistance)
    private val zMultiplier = (farDistance + nearDistance) * rangeReciprocal
    private val zOffset = 2 * farDistance * nearDistance * rangeReciprocal

    private val linear = LinearCameraAnglePixelMapper()

    override fun getAngle(
        x: Float,
        y: Float,
        imageRect: RectF,
        fieldOfView: Size
    ): Vector2 {
        // TODO: Inverse perspective?
        return linear.getAngle(x, y, imageRect, fieldOfView)
    }

    override fun getPixel(
        angleX: Float,
        angleY: Float,
        imageRect: RectF,
        fieldOfView: Size,
        distance: Float?
    ): PixelCoordinate {
        val world = toCartesian(angleX, angleY, distance ?: farDistance)

        // Point is behind the camera, so calculate the linear projection
        if (world.z < 0) {
            return linear.getPixel(angleX, angleY, imageRect, fieldOfView, distance)
        }

        // Perspective matrix multiplication - written out to avoid unnecessary allocations and calculations
        val f = 1 / SolMath.tanDegrees(fieldOfView.height / 2)

        val aspect = fieldOfView.width / fieldOfView.height
        val x = f / aspect * world.x
        val y = f * world.y
        var z = zMultiplier * world.z + zOffset
        if (z == 0f) {
            // Prevent NaN
            z = 1f
        }

        val screenX = x / z
        val screenY = y / z

        val pixelX = (1 - screenX) / 2f * imageRect.width() + imageRect.left
        val pixelY = (screenY + 1) / 2f * imageRect.height() + imageRect.top

        return PixelCoordinate(pixelX, pixelY)
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