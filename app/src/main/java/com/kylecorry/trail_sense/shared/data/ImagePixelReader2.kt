package com.kylecorry.trail_sense.shared.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.get
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.bitmaps.BitmapUtils.isInBounds
import com.kylecorry.andromeda.bitmaps.BitmapUtils.use
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Range
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
    private val lookupOrder: Int = 1, // The layers of surrounding pixels to look up (1 = 4 pixels, 2 = 12 pixels, etc.)
    private val returnAllPixels: Boolean = false
) {

    suspend fun getRegion(
        image: InputStream,
        region: Rect,
        exact: Boolean = false,
        autoClose: Boolean = true
    ): Pair<Rect, Bitmap>? =
        onIO {
            try {
                val rect = BitmapUtils.getExactRegion(region, imageSize)
                val bitmap = BitmapUtils.decodeRegion(
                    image,
                    rect,
                    BitmapFactory.Options().also { it.inPreferredConfig = config },
                    autoClose = false
                ) ?: return@onIO null

                if (bitmap.width <= 0 || bitmap.height <= 0) {
                    bitmap.recycle()
                    return@onIO null
                }

                // Crop the bitmap to the original region
                if (exact && rect != region) {
                    var cropped: Bitmap? = null
                    bitmap.use {
                        // TODO: Handle when the original region was out of bounds
                        cropped = Bitmap.createBitmap(
                            bitmap,
                            region.left - rect.left,
                            region.top - rect.top,
                            region.width(),
                            region.height()
                        )
                    }
                    cropped ?: return@onIO null
                    region to cropped
                } else {
                    rect to bitmap
                }
            } finally {
                if (autoClose) {
                    image.close()
                }
            }
        }

    suspend fun getAllPixels(
        image: InputStream,
        pixels: List<PixelCoordinate>,
        autoClose: Boolean = true
    ): List<PixelResult<Int>> = onIO {
        var bitmap: Bitmap? = null
        try {
            if (pixels.isEmpty()) {
                return@onIO emptyList()
            }

            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE
            for (pixel in pixels) {
                if (pixel.x < minX) {
                    minX = pixel.x
                }
                if (pixel.x > maxX) {
                    maxX = pixel.x
                }
                if (pixel.y < minY) {
                    minY = pixel.y
                }
                if (pixel.y > maxY) {
                    maxY = pixel.y
                }
            }

            val xRange = Range(minX, maxX)
            val yRange = Range(minY, maxY)

            val xStart = floor(xRange.start).toInt()
            val yStart = floor(yRange.start).toInt()
            val xEnd = ceil(xRange.end).toInt()
            val yEnd = ceil(yRange.end).toInt()

            val (rect, loadedBitmap) = getRegion(
                image, Rect(
                    xStart - lookupOrder * 2,
                    yStart - lookupOrder * 2,
                    xEnd + lookupOrder * 2,
                    yEnd + lookupOrder * 2
                ), false
            ) ?: return@onIO emptyList()
            bitmap = loadedBitmap

            val allPixels = mutableListOf<PixelResult<Int>>()
            if (returnAllPixels) {
                for (x in 0 until bitmap.width) {
                    for (y in 0 until bitmap.height) {
                        allPixels.add(PixelResult(x + rect.left, y + rect.top, bitmap[x, y]))
                    }
                }
                allPixels
            } else {
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
            }
        } finally {
            bitmap?.recycle()
            if (autoClose) {
                image.close()
            }
        }
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