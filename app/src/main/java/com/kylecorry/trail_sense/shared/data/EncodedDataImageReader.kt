package com.kylecorry.trail_sense.shared.data

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.bitmaps.BitmapUtils.use
import com.kylecorry.sol.math.SolMath
import com.kylecorry.trail_sense.shared.andromeda_temp.getPixels

class EncodedDataImageReader(
    private val reader: ImageReader,
    private val treatZeroAsNaN: Boolean = false,
    private val maxChannels: Int? = null,
    private val decoder: (Int) -> FloatArray = { floatArrayOf(it.toFloat()) }
) : DataImageReader {
    override fun getSize(): Size {
        return reader.getSize()
    }

    override suspend fun getRegion(rect: Rect, config: Bitmap.Config): Pair<FloatBitmap, Boolean>? {
        var pixelGrid: FloatBitmap? = null
        var isAllNaN = true
        reader.getRegion(rect)?.use {
            val pixels = getPixels()
            val w = width
            val h = height
            val channels = if (maxChannels != null) {
                val firstPixel = if (pixels.isNotEmpty()) decoder(pixels[0]) else floatArrayOf()
                minOf(firstPixel.size, maxChannels)
            } else {
                if (pixels.isNotEmpty()) decoder(pixels[0]).size else 0
            }

            pixelGrid = FloatBitmap(w, h, channels)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val pixel = pixels[y * w + x]
                    val decoded = decoder(pixel)
                    for (i in 0 until channels) {
                        val value = if (i < decoded.size) decoded[i] else Float.NaN
                        val finalValue =
                            if ((treatZeroAsNaN && SolMath.isZero(value)) || value.isNaN()) {
                                Float.NaN
                            } else {
                                isAllNaN = false
                                value
                            }
                        pixelGrid.set(x, y, i, finalValue)
                    }
                }
            }
        }

        if (pixelGrid == null) {
            return null
        }

        return pixelGrid to !isAllNaN
    }

    companion object {

        fun scaledDecoder(
            a: Double,
            b: Double,
            convertZero: Boolean = true
        ): (Int) -> FloatArray {
            return {
                val red = it.red.toFloat()
                val green = it.green.toFloat()
                val blue = it.blue.toFloat()
                val alpha = it.alpha.toFloat()

                if (!convertZero && red == 0f && green == 0f && blue == 0f) {
                    floatArrayOf(0f, 0f, 0f, alpha)
                } else {
                    floatArrayOf(
                        (red / a - b).toFloat(),
                        (green / a - b).toFloat(),
                        (blue / a - b).toFloat(),
                        (alpha / a - b).toFloat()
                    )
                }
            }
        }

        fun split16BitDecoder(a: Double = 1.0, b: Double = 0.0): (Int) -> FloatArray {
            return {
                val red = it.red
                val green = it.green
                val blue = it.blue
                val alpha = it.alpha

                floatArrayOf(
                    ((green shl 8 or red).toDouble() / a - b).toFloat(),
                    ((alpha shl 8 or blue).toDouble() / a - b).toFloat()
                )

            }
        }

    }
}