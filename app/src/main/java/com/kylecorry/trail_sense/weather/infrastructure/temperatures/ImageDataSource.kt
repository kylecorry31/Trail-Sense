package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.InputStream

class ImageDataSource(
    private val imageSize: Size,
    private val samplingSize: Int = 1,
    private val samplingCenterWeight: Int = 2,
    private val samplingPixelFilter: (Int) -> Boolean = { true }
) {

    suspend fun getPixel(
        image: InputStream,
        x: Int,
        y: Int,
        closeStream: Boolean = true
    ): Int? = onIO {
        var bitmap: Bitmap? = null
        try {
            val decoder = getDecoder(image) ?: return@onIO null
            val options = BitmapFactory.Options()
            bitmap = decoder.decodeRegion(getRegion(x, y), options)

            var sumR = 0
            var sumG = 0
            var sumB = 0
            var sumA = 0
            var count = 0
            for (i in 0 until bitmap.width) {
                for (j in 0 until bitmap.height) {
                    val pixel = bitmap[i, j]
                    val red = pixel.red
                    val green = pixel.green
                    val blue = pixel.blue
                    val alpha = pixel.alpha

                    if (samplingPixelFilter(pixel)) {
                        val weight = if (i == x && j == y) samplingCenterWeight else 1
                        sumR += red * weight
                        sumG += green * weight
                        sumB += blue * weight
                        sumA += alpha * weight
                        count += weight
                    }
                }
            }

            if (count == 0) {
                return@onIO null
            }

            return@onIO Color.argb(
                (sumA / count.toFloat()).toInt(),
                (sumR / count.toFloat()).toInt(),
                (sumG / count.toFloat()).toInt(),
                (sumB / count.toFloat()).toInt()
            )
        } finally {
            bitmap?.recycle()
            if (closeStream) {
                image.close()
            }
        }
    }

    private fun getRegion(x: Int, y: Int): Rect {

        val offset = samplingSize / 2

        val left = x - offset
        val top = y - offset
        val bottom = y + offset + 1
        val right = x + offset + 1

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

}