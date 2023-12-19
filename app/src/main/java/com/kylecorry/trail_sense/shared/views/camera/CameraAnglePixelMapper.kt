package com.kylecorry.trail_sense.shared.views.camera

import android.graphics.RectF
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Size

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
}