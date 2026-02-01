package com.kylecorry.trail_sense.shared.data

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.bitmaps.BitmapUtils.use
import com.kylecorry.andromeda.bitmaps.FloatBitmap
import com.kylecorry.sol.math.SolMath
import com.kylecorry.trail_sense.shared.andromeda_temp.getPixels

class EncodedDataImageReader(
    private val reader: ImageReader,
    private val treatZeroAsNaN: Boolean = false,
    private val maxChannels: Int? = null,
    private val decoder: PixelDecoder = defaultDecoder
) : DataImageReader {

    override val channels: Int = maxChannels ?: 4

    interface PixelDecoder {
        val channels: Int
        fun decode(pixel: Int, dest: FloatArray)
    }

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
                minOf(decoder.channels, maxChannels)
            } else {
                decoder.channels
            }

            pixelGrid = FloatBitmap(w, h, channels)
            val data = pixelGrid.data
            val buffer = FloatArray(decoder.channels)
            var idx = 0

            for (y in 0 until h) {
                for (x in 0 until w) {
                    val pixel = pixels[y * w + x]
                    decoder.decode(pixel, buffer)
                    for (i in 0 until channels) {
                        val value = buffer[i]
                        val finalValue =
                            if ((treatZeroAsNaN && SolMath.isZero(value)) || value.isNaN()) {
                                Float.NaN
                            } else {
                                isAllNaN = false
                                value
                            }
                        data[idx++] = finalValue
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

        val defaultDecoder = object : PixelDecoder {
            override val channels = 1
            override fun decode(pixel: Int, dest: FloatArray) {
                dest[0] = pixel.toFloat()
            }
        }

        fun scaledDecoder(
            a: Double,
            b: Double,
            convertZero: Boolean = true
        ): PixelDecoder {
            return object : PixelDecoder {
                override val channels = 4
                override fun decode(pixel: Int, dest: FloatArray) {
                    val red = pixel.red.toFloat()
                    val green = pixel.green.toFloat()
                    val blue = pixel.blue.toFloat()
                    val alpha = pixel.alpha.toFloat()

                    if (!convertZero && red == 0f && green == 0f && blue == 0f) {
                        dest[0] = 0f
                        dest[1] = 0f
                        dest[2] = 0f
                        dest[3] = alpha
                    } else {
                        dest[0] = (red / a - b).toFloat()
                        dest[1] = (green / a - b).toFloat()
                        dest[2] = (blue / a - b).toFloat()
                        dest[3] = (alpha / a - b).toFloat()
                    }
                }
            }
        }

        fun split16BitDecoder(a: Double = 1.0, b: Double = 0.0): PixelDecoder {
            return object : PixelDecoder {
                override val channels = 2
                override fun decode(pixel: Int, dest: FloatArray) {
                    val red = pixel.red
                    val green = pixel.green
                    val blue = pixel.blue
                    val alpha = pixel.alpha

                    dest[0] = ((green shl 8 or red).toDouble() / a - b).toFloat()
                    dest[1] = ((alpha shl 8 or blue).toDouble() / a - b).toFloat()
                }
            }
        }

    }
}