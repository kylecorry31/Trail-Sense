package com.kylecorry.trail_sense.shared.data

import android.graphics.Rect
import com.kylecorry.andromeda.core.units.PixelCoordinate

class FloatBitmapInterpolator(interpolationOrder: Int) {

    val interpolators = listOfNotNull(
        if (interpolationOrder == 2) BicubicInterpolator() else null,
        if (interpolationOrder == 1) BilinearInterpolator() else null,
        NearestInterpolator()
    )

    fun getValue(bitmap: FloatBitmap, rect: Rect, x: Float, y: Float): FloatArray {
        if (bitmap.isEmpty() || bitmap[0].isEmpty() || bitmap[0][0].isEmpty()) {
            return FloatArray(0)
        }

        val interpolated = FloatArray(bitmap[0][0].size)

        for (i in interpolated.indices) {
            val localPixel = PixelCoordinate(
                x - rect.left,
                y - rect.top
            )
            interpolated[i] = interpolators.firstNotNullOfOrNull {
                it.interpolate(localPixel, bitmap, i)
            } ?: 0f
        }

        return interpolated
    }
}