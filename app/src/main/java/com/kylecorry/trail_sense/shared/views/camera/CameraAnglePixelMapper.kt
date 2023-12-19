package com.kylecorry.trail_sense.shared.views.camera

import android.graphics.RectF
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2

interface CameraAnglePixelMapper {
    fun getAngle(x: Float, y: Float, previewRect: RectF, previewFOV: Pair<Float, Float>): Vector2
    fun getPixel(
        angleX: Float,
        angleY: Float,
        previewRect: RectF,
        previewFOV: Pair<Float, Float>,
        distance: Float? = null
    ): PixelCoordinate
}