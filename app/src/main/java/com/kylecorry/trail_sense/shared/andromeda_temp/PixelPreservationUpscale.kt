package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation

/**
 * An upscaler that preserves the colors of the pixels but attempts to smooth edges
 */
class PixelPreservationUpscale(
    private val size: Size
) : BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        var array = bitmap.getPixels()
        var width = bitmap.width
        var height = bitmap.height
        while (width < size.width && height < size.height) {
            array = upscale2x(array, width, height)
            width *= 2
            height *= 2
        }
        return Bitmap.createBitmap(array, width, height, bitmap.config)
    }

    private fun upscale2x(src: IntArray, width: Int, height: Int): IntArray {
        val destinationWidth = width * 2
        val destinationHeight = height * 2
        val destination = IntArray(destinationWidth * destinationHeight)

        fun get(x: Int, y: Int): Int {
            val sx = x.coerceIn(0, width - 1)
            val sy = y.coerceIn(0, height - 1)
            return src.get(sx, sy, width)
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val center = get(x, y)
                val top = get(x, y - 1)
                val bottom = get(x, y + 1)
                val left = get(x - 1, y)
                val right = get(x + 1, y)

                val topLeft = get(x - 1, y - 1)
                val topRight = get(x + 1, y - 1)
                val bottomLeft = get(x - 1, y + 1)
                val bottomRight = get(x + 1, y + 1)

                val targetTopLeft =
                    if (left != right && top != bottom && topLeft != center) topLeft else center
                val targetTopRight =
                    if (left != right && top != bottom && topRight != center) topRight else center
                val targetBottomLeft =
                    if (left != right && top != bottom && bottomLeft != center) bottomLeft else center
                val targetBottomRight =
                    if (left != right && top != bottom && bottomRight != center) bottomRight else center

                val index = (y * 2) * destinationWidth + (x * 2)
                destination[index] = targetTopLeft
                destination[index + 1] = targetTopRight
                destination[index + destinationWidth] = targetBottomLeft
                destination[index + destinationWidth + 1] = targetBottomRight
            }
        }

        return destination
    }
}