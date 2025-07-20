package com.kylecorry.trail_sense.shared.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.core.graphics.get
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.bitmaps.BitmapUtils.isInBounds
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.extensions.range
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.getExactRegion
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.floor

class PixelResult<T>(
    val x: Int,
    val y: Int,
    val value: T
) {
    val coordinate: PixelCoordinate
        get() = PixelCoordinate(x.toFloat(), y.toFloat())
}

/**
 * Read a pixel from an image without loading the entire image into memory
 * @param imageSize The size of the image
 * @param config The bitmap config to use
 */
class ImagePixelReader2(
    private val imageSize: Size,
    private val config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    private val lookupOrder: Int = 1 // The layers of surrounding pixels to look up (1 = 4 pixels, 2 = 12 pixels, etc.)
) {

    suspend fun getAllPixels(
        image: InputStream,
        pixels: List<PixelCoordinate>,
        autoClose: Boolean = true
    ): List<PixelResult<Int>> = onIO {
        var bitmap: Bitmap? = null
        try {
            val xRange = pixels.map { it.x }.range() ?: return@onIO emptyList()
            val yRange = pixels.map { it.y }.range() ?: return@onIO emptyList()

            val xStart = floor(xRange.start).toInt()
            val yStart = floor(yRange.start).toInt()
            val xEnd = ceil(xRange.end).toInt()
            val yEnd = ceil(yRange.end).toInt()

            val rect = BitmapUtils.getExactRegion(
                Rect(
                    xStart - lookupOrder * 2,
                    yStart - lookupOrder * 2,
                    xEnd + lookupOrder * 2,
                    yEnd + lookupOrder * 2
                ),
                imageSize
            )
            bitmap = BitmapUtils.decodeRegion(
                image,
                rect,
                BitmapFactory.Options().also { it.inPreferredConfig = config },
                autoClose = false
            ) ?: return@onIO emptyList()

            if (bitmap.width != rect.width() ||
                bitmap.height != rect.height()
            ) {
                Log.w(
                    "ImagePixelReader",
                    "Bitmap size does not match expected region size. Expected: ${rect.width()}x${rect.height()}, Actual: ${bitmap.width}x${bitmap.height}"
                )
            }

            if (bitmap.width <= 0 || bitmap.height <= 0) {
                return@onIO emptyList()
            }

            val allPixels = mutableListOf<PixelResult<Int>>()
            for (pixel in pixels) {
                val newX = pixel.x - rect.left
                val newY = pixel.y - rect.top

                allPixels.addAll((1..lookupOrder).flatMap { order ->
                    getNearbyPixels(newX.toInt(), newY.toInt(), order, bitmap)
                }.map {
                    PixelResult(
                        it.x + rect.left,
                        it.y + rect.top,
                        it.value
                    )
                })
            }
            allPixels.distinctBy { it.x to it.y }
        } finally {
            bitmap?.recycle()
            if (autoClose) {
                image.close()
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getPixels(
        image: InputStream,
        x: Float,
        y: Float,
        autoClose: Boolean = true
    ): List<PixelResult<Int>> = onIO {
        getAllPixels(image, listOf(PixelCoordinate(x, y)), autoClose)
    }

    private fun getNearbyPixels(
        x: Int,
        y: Int,
        order: Int,
        bitmap: Bitmap
    ): List<PixelResult<Int>> {
        // Order 1 means the surrounding 4 pixels, order 2 is the next layer (the 10 around that)
        val startX = x - (order - 1)
        val endX = x + order
        val startY = y - (order - 1)
        val endY = y + order

        val pixels = mutableListOf<PixelResult<Int>>()
        for (i in startX..endX) {
            // Top row
            if (bitmap.isInBounds(i, startY)) {
                pixels.add(PixelResult(i, startY, bitmap[i, startY]))
            }
            // Bottom row
            if (bitmap.isInBounds(i, endY)) {
                pixels.add(PixelResult(i, endY, bitmap[i, endY]))
            }
        }
        for (j in (startY + 1) until endY) {
            // Left column
            if (bitmap.isInBounds(startX, j)) {
                pixels.add(PixelResult(startX, j, bitmap[startX, j]))
            }
            // Right column
            if (bitmap.isInBounds(endX, j)) {
                pixels.add(PixelResult(endX, j, bitmap[endX, j]))
            }
        }
        return pixels
    }
}