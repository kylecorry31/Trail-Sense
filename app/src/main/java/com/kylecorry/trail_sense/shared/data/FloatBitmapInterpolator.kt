package com.kylecorry.trail_sense.shared.data

import android.graphics.Rect
import com.kylecorry.andromeda.core.units.PixelCoordinate

class FloatBitmapInterpolator(interpolationOrder: Int) {

    val interpolators = listOfNotNull(
        if (interpolationOrder == 2) BicubicInterpolator() else null,
        if (interpolationOrder == 1) BilinearInterpolator() else null,
        NearestInterpolator()
    )

    fun getValue(
        bitmap: FloatBitmap,
        rect: Rect,
        x: Float,
        y: Float,
        dest: FloatArray,
        destOffset: Int = 0
    ) {
        if (bitmap.width == 0 || bitmap.height == 0 || bitmap.channels == 0) {
            return
        }

        val localPixel = PixelCoordinate(
            x - rect.left,
            y - rect.top
        )

        for (i in 0 until bitmap.channels) {
            dest[destOffset + i] = interpolators.firstNotNullOfOrNull {
                it.interpolate(localPixel, bitmap, i)
            } ?: 0f
        }
    }

    fun getValue(bitmap: FloatBitmap, rect: Rect, x: Float, y: Float): FloatArray {
        val interpolated = FloatArray(bitmap.channels)
        getValue(bitmap, rect, x, y, interpolated)
        return interpolated
    }
}