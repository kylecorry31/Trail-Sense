package com.kylecorry.trail_sense.shared.views.camera

import android.graphics.RectF
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.shared.views.camera.CameraAnglePixelMapper

class LinearCameraAnglePixelMapper: CameraAnglePixelMapper {
    override fun getAngle(
        x: Float,
        y: Float,
        previewRect: RectF,
        previewFOV: Pair<Float, Float>
    ): Vector2 {
        val xAngle = ((x - previewRect.centerX()) / previewRect.width()) * previewFOV.first
        val yAngle = -((y - previewRect.centerY()) / previewRect.height()) * previewFOV.second
        return Vector2(xAngle, yAngle)
    }

    override fun getPixel(
        angleX: Float,
        angleY: Float,
        previewRect: RectF,
        previewFOV: Pair<Float, Float>,
        distance: Float?
    ): PixelCoordinate {
//        val x = (angleX / previewFOV.first) * previewRect.width() + previewRect.centerX()
//        val y = (-angleY / previewFOV.second) * previewRect.height() + previewRect.centerY()
//        return PixelCoordinate(x, y)

        val wPixelsPerDegree = previewRect.width() / previewFOV.first
        val hPixelsPerDegree = previewRect.height() / previewFOV.second

        val x = previewRect.centerX() + angleX * wPixelsPerDegree
        val y = previewRect.centerY() - angleY * hPixelsPerDegree

        return PixelCoordinate(x, y)
    }
}