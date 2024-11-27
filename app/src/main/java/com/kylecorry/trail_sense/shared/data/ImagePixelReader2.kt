package com.kylecorry.trail_sense.shared.data

import com.kylecorry.andromeda.core.bitmap.BitmapUtils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Size
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.interpolateBilinear
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.nearestPixel
import com.kylecorry.andromeda.core.coroutines.onIO
import java.io.InputStream
import kotlin.math.roundToInt

/**
 * Read a pixel from an image without loading the entire image into memory
 * @param imageSize The size of the image
 * @param interpolate Whether to interpolate between pixels
 */
class ImagePixelReader2(
    private val imageSize: Size,
    private val interpolate: Boolean = true,
    private val config: Bitmap.Config = Bitmap.Config.ARGB_8888
) {

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getPixel(
        image: InputStream,
        x: Float,
        y: Float,
        autoClose: Boolean = true
    ): Int? = onIO {
        var bitmap: Bitmap? = null
        try {
            val rect = getRegion(x.roundToInt(), y.roundToInt())
            bitmap = BitmapUtils.decodeRegion(image, rect, BitmapFactory.Options().also {
                it.inPreferredConfig = config
            }, autoClose = false) ?: return@onIO null

            if (bitmap.width <= 0 || bitmap.height <= 0) {
                return@onIO null
            }

            val newX = x - rect.left
            val newY = y - rect.top

            return@onIO if (!interpolate) {
                bitmap.nearestPixel(newX, newY)
            } else {
                bitmap.interpolateBilinear(newX, newY) ?: bitmap.nearestPixel(newX, newY)
            }
        } finally {
            bitmap?.recycle()
            if (autoClose) {
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

}