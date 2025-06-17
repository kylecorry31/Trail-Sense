package com.kylecorry.trail_sense.shared.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.get
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.bitmaps.BitmapUtils.isInBounds
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.sumOfFloat
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Read a pixel from an image without loading the entire image into memory
 * @param imageSize The size of the image
 * @param config The bitmap config to use
 */
class ImagePixelReader2(
    private val imageSize: Size,
    private val config: Bitmap.Config = Bitmap.Config.ARGB_8888
) {

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getPixels(
        image: InputStream,
        x: Float,
        y: Float,
        autoClose: Boolean = true
    ): List<Pair<PixelCoordinate, Int>> = onIO {
        var bitmap: Bitmap? = null
        try {
            val rect = getRegion(x.roundToInt(), y.roundToInt())
            bitmap = BitmapUtils.decodeRegion(
                image,
                rect,
                BitmapFactory.Options().also { it.inPreferredConfig = config },
                autoClose = false
            ) ?: return@onIO emptyList()

            if (bitmap.width <= 0 || bitmap.height <= 0) {
                return@onIO emptyList()
            }

            val newX = x - rect.left
            val newY = y - rect.top

            val x1 = newX.toInt()
            val x2 = x1 + 1
            val y1 = newY.toInt()
            val y2 = y1 + 1

            if (!bitmap.isInBounds(x1, y1) || !bitmap.isInBounds(x2, y2)) {
                return@onIO emptyList()
            }

            val x1y1 = bitmap[x1, y1]
            val x1y2 = bitmap[x1, y2]
            val x2y1 = bitmap[x2, y1]
            val x2y2 = bitmap[x2, y2]

            return@onIO listOf(
                PixelCoordinate(x1.toFloat() + rect.left, y1.toFloat() + rect.top) to x1y1,
                PixelCoordinate(x1.toFloat() + rect.left, y2.toFloat() + rect.top) to x1y2,
                PixelCoordinate(x2.toFloat() + rect.left, y1.toFloat() + rect.top) to x2y1,
                PixelCoordinate(x2.toFloat() + rect.left, y2.toFloat() + rect.top) to x2y2
            )
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

    companion object {
        fun interpolate(point: PixelCoordinate, values: List<Pair<PixelCoordinate, Float>>): Float {
            val weights =
                values.map { (abs(it.first.x - point.x) * abs(it.first.y - point.y)) to it.second }

            return weights.sumOfFloat { it.first * it.second } /
                    weights.sumOfFloat { it.first }
        }
    }

}