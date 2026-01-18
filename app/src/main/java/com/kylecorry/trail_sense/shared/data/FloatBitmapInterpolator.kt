package com.kylecorry.trail_sense.shared.data

import android.graphics.Rect
import com.kylecorry.andromeda.core.units.PixelCoordinate
import kotlin.math.max

class FloatBitmapInterpolator(
    private val interpolationOrder: Int,
    private val pixelProvider: CrossBoundaryPixelProvider? = null
) {

    suspend fun getValue(
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
            dest[destOffset + i] = interpolate(localPixel, bitmap, rect, i)
        }
    }

    suspend fun getValue(bitmap: FloatBitmap, rect: Rect, x: Float, y: Float): FloatArray {
        val interpolated = FloatArray(bitmap.channels)
        getValue(bitmap, rect, x, y, interpolated)
        return interpolated
    }

    private suspend fun interpolate(
        localPixel: PixelCoordinate,
        bitmap: FloatBitmap,
        rect: Rect,
        channel: Int
    ): Float {
        val pixelProvider = pixelProvider ?: return fallbackInterpolate(localPixel, bitmap, channel)
        val interpolator = when (interpolationOrder) {
            2 -> BicubicInterpolator()
            1 -> BilinearInterpolator()
            else -> NearestInterpolator(max(bitmap.width, bitmap.height))
        }

        return interpolator.interpolate(localPixel) { x, y ->
            val value = bitmap.getOrNull(x, y, channel)
            if (value != null && !value.isNaN()) {
                value
            } else {
                val globalX = x + rect.left
                val globalY = y + rect.top
                pixelProvider.getPixel(globalX, globalY, channel)
            }
        } ?: fallbackInterpolate(localPixel, bitmap, channel)
    }

    private suspend fun fallbackInterpolate(
        localPixel: PixelCoordinate,
        bitmap: FloatBitmap,
        channel: Int
    ): Float {
        return listOfNotNull(
            if (interpolationOrder == 2) BicubicInterpolator() else null,
            if (interpolationOrder == 1) BilinearInterpolator() else null,
            NearestInterpolator(max(bitmap.width, bitmap.height))
        ).firstNotNullOfOrNull {
            it.interpolate(localPixel) { x, y -> bitmap.getOrNull(x, y, channel) }
        } ?: 0f
    }
}
