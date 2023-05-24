package com.kylecorry.trail_sense.shared.io

import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.graphics.Color
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import java.io.InputStream
import kotlin.math.roundToInt

/**
 * A data source backed by an image
 * @param imageSize The size of the image
 * @param interpolate Whether to interpolate between pixels
 */
class ImageDataSource(
    private val imageSize: Size,
    private val interpolate: Boolean = true,
) {

    suspend fun getPixel(
        image: InputStream,
        position: PixelCoordinate,
        closeStream: Boolean = true
    ): Int? = onIO {
        var bitmap: Bitmap? = null
        try {
            val decoder = getDecoder(image) ?: return@onIO null
            val rect = getRegion(position.x.roundToInt(), position.y.roundToInt())
            bitmap = decoder.decodeRegion(rect, null) ?: return@onIO null

            val recenteredPosition = PixelCoordinate(
                position.x - rect.left,
                position.y - rect.top
            )

            return@onIO if (!interpolate) {
                bitmap[recenteredPosition.x.roundToInt(), recenteredPosition.y.roundToInt()]
            } else {
                bilinearInterpolation(recenteredPosition, bitmap)
            }
        } finally {
            bitmap?.recycle()
            if (closeStream) {
                image.close()
            }
        }
    }

    private fun getRegion(x: Int, y: Int): Rect {

        val width = 6

        // Always start/end on an even pixel or else it seems to be off by 1
        val left = x - 2 - (x % 2)
        val top = y - 2 - (y % 2)
        val right = left + width
        val bottom = top + width

        return Rect(
            left.coerceIn(0, imageSize.width),
            top.coerceIn(0, imageSize.height),
            right.coerceIn(0, imageSize.width),
            bottom.coerceIn(0, imageSize.height)
        )
    }

    private fun getDecoder(stream: InputStream): BitmapRegionDecoder? {
        return if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.R) {
            BitmapRegionDecoder.newInstance(stream)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(stream, false)
        }
    }

    private fun bilinearInterpolation(position: PixelCoordinate, bitmap: Bitmap): Int {
        val x = position.x
        val y = position.y

        val x1 = x.toInt()
        val x2 = x1 + 1
        val y1 = y.toInt()
        val y2 = y1 + 1

        val x1y1 = bitmap[x1, y1]
        val x1y2 = bitmap[x1, y2]
        val x2y1 = bitmap[x2, y1]
        val x2y2 = bitmap[x2, y2]

        val x1y1Weight = (x2 - x) * (y2 - y)
        val x1y2Weight = (x2 - x) * (y - y1)
        val x2y1Weight = (x - x1) * (y2 - y)
        val x2y2Weight = (x - x1) * (y - y1)

        val red = x1y1.red * x1y1Weight + x1y2.red * x1y2Weight + x2y1.red * x2y1Weight + x2y2.red * x2y2Weight
        val green = x1y1.green * x1y1Weight + x1y2.green * x1y2Weight + x2y1.green * x2y1Weight + x2y2.green * x2y2Weight
        val blue = x1y1.blue * x1y1Weight + x1y2.blue * x1y2Weight + x2y1.blue * x2y1Weight + x2y2.blue * x2y2Weight
        val alpha = x1y1.alpha * x1y1Weight + x1y2.alpha * x1y2Weight + x2y1.alpha * x2y1Weight + x2y2.alpha * x2y2Weight

        return Color.argb(alpha.toInt(), red.toInt(), green.toInt(), blue.toInt())
    }

}