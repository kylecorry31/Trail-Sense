package com.kylecorry.trail_sense.tools.augmented_reality.domain.mapper

import android.graphics.RectF
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size

/**
 * A camera angle pixel mapper that uses a linear projection to map angles to pixels.
 */
class LinearCameraAnglePixelMapper : CameraAnglePixelMapper {
    override fun getAngle(
        x: Float,
        y: Float,
        imageRect: RectF,
        fieldOfView: Size
    ): Vector2 {
        val xAngle = ((x - imageRect.centerX()) / imageRect.width()) * fieldOfView.width
        val yAngle = -((y - imageRect.centerY()) / imageRect.height()) * fieldOfView.height
        return Vector2(xAngle, yAngle)
    }

    override fun getPixel(
        angleX: Float,
        angleY: Float,
        imageRect: RectF,
        fieldOfView: Size,
        distance: Float?
    ): PixelCoordinate {
        val x = (angleX / fieldOfView.width) * imageRect.width() + imageRect.centerX()
        val y = (-angleY / fieldOfView.height) * imageRect.height() + imageRect.centerY()
        return PixelCoordinate(x, y)
    }

    override fun getPixel(world: Vector3, imageRect: RectF, fieldOfView: Size): PixelCoordinate {
        val spherical = CameraAnglePixelMapper.toSpherical(world)
        return getPixel(spherical.z, spherical.y, imageRect, fieldOfView, spherical.x)
    }
}