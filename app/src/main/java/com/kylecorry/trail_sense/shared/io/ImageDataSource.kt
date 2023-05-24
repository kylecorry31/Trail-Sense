package com.kylecorry.trail_sense.shared.io

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
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.io.ImageDataSource.ImageDataSourceSampler
import java.io.InputStream
import kotlin.math.sqrt

/**
 * A data source backed by an image
 * @param imageSize The size of the image
 * @param samplingSize The size of the sampling area
 * @param sampler A function that determines the weight of a pixel in the sampling area. The first parameter is position being looked up, the second is current pixel position for sampling, and the third is the pixel color.
 */
class ImageDataSource(
    private val imageSize: Size,
    private val samplingSize: Int = 1,
    private val sampler: ImageDataSourceSampler = uniformSampler()
) {

    suspend fun getPixel(
        image: InputStream,
        position: PixelCoordinate,
        closeStream: Boolean = true
    ): Int? = onIO {
        var bitmap: Bitmap? = null
        try {
            val decoder = getDecoder(image) ?: return@onIO null
            val options = BitmapFactory.Options()
            val rect = getRegion(position.x.toInt(), position.y.toInt())
            bitmap = decoder.decodeRegion(rect, options)

            var sumR = 0f
            var sumG = 0f
            var sumB = 0f
            var sumA = 0f
            var totalWeight = 0f


            for (i in 0 until bitmap.width) {
                for (j in 0 until bitmap.height) {
                    val pixel = bitmap[i, j]
                    val red = pixel.red
                    val green = pixel.green
                    val blue = pixel.blue
                    val alpha = pixel.alpha

                    val weight = sampler.getWeight(
                        position,
                        PixelCoordinate(i.toFloat() + rect.left, j.toFloat() + rect.top),
                        pixel
                    )

                    sumR += red * weight
                    sumG += green * weight
                    sumB += blue * weight
                    sumA += alpha * weight
                    totalWeight += weight
                }
            }

            if (totalWeight == 0f) {
                return@onIO null
            }

            return@onIO Color.argb(
                (sumA / totalWeight).toInt(),
                (sumR / totalWeight).toInt(),
                (sumG / totalWeight).toInt(),
                (sumB / totalWeight).toInt()
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
        val bottom = y + if (samplingSize == 1) 1 else offset
        val right = x + if (samplingSize == 1) 1 else offset

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

    fun interface ImageDataSourceSampler {
        fun getWeight(position: PixelCoordinate, current: PixelCoordinate, color: Int): Float
    }

    companion object {
        fun uniformSampler(isColorValid: (Int) -> Boolean = { true }): ImageDataSourceSampler {
            return ImageDataSourceSampler { _, _, color ->
                if (!isColorValid(color)) {
                    0f
                } else {
                    1f
                }
            }
        }

        /**
         * A sampler based on geographic distance
         * @param latitudeBias Determines how much latitude affects the weight. 0 means no effect. distance multiplier = 1 / bias
         * @param longitudeBias Determines how much longitude affects the weight. 0 means no effect. distance multiplier = 1 / bias
         * @param isColorValid A function that determines if a color is valid (invalid receives a weight of 0)
         */
        fun geographicSampler(
            latitudeBias: Float = 1f,
            longitudeBias: Float = 1f,
            isColorValid: (Int) -> Boolean = { true }
        ): ImageDataSourceSampler {
            return ImageDataSourceSampler { position, current, color ->

                if (!isColorValid(color)) {
                    return@ImageDataSourceSampler 0f
                }

                val xDistance =
                    (current.x - position.x) * if (longitudeBias == 0f) 0f else 1 / longitudeBias
                val yDistance =
                    (current.y - position.y) * if (latitudeBias == 0f) 0f else 1 / latitudeBias

                val distance = sqrt(xDistance * xDistance + yDistance * yDistance)

                println(1 / (1 + distance))

                1 / (1 + distance)
            }
        }

    }

}