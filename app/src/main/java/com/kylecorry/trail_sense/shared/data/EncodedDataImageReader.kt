package com.kylecorry.trail_sense.shared.data

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.bitmaps.BitmapUtils.use
import com.kylecorry.sol.math.SolMath

class EncodedDataImageReader(
    private val reader: ImageReader,
    private val treatZeroAsNaN: Boolean = false,
    private val maxChannels: Int? = null,
    private val decoder: (Int?) -> List<Float> = { listOf(it?.toFloat() ?: 0f) }
) : DataImageReader {
    override fun getSize(): Size {
        return reader.getSize()
    }

    override suspend fun getRegion(rect: Rect, config: Bitmap.Config): Pair<FloatBitmap, Boolean>? {
        var pixelGrid: Array<Array<FloatArray>>? = null
        var isAllNaN = true
        reader.getRegion(rect)?.use {
            pixelGrid = Array(height) { y ->
                Array(width) { x ->
                    val decoded = decoder(this[x, y])
                    val channels = if (maxChannels != null) minOf(
                        decoded.size,
                        maxChannels
                    ) else decoded.size
                    FloatArray(channels) { i ->
                        val value = decoded[i]
                        if ((treatZeroAsNaN && SolMath.isZero(value)) || value.isNaN()) {
                            Float.NaN
                        } else {
                            isAllNaN = false
                            value
                        }
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
        ): (Int?) -> List<Float> {
            return {
                val red = it?.red?.toFloat() ?: 0f
                val green = it?.green?.toFloat() ?: 0f
                val blue = it?.blue?.toFloat() ?: 0f
                val alpha = it?.alpha?.toFloat() ?: 0f

                if (!convertZero && red == 0f && green == 0f && blue == 0f) {
                    listOf(0f, 0f, 0f, alpha)
                } else {
                    listOf(
                        red / a - b,
                        green / a - b,
                        blue / a - b,
                        alpha / a - b
                    ).map { it.toFloat() }
                }
            }
        }

        fun split16BitDecoder(a: Double = 1.0, b: Double = 0.0): (Int?) -> List<Float> {
            return {
                val red = it?.red ?: 0
                val green = it?.green ?: 0
                val blue = it?.blue ?: 0
                val alpha = it?.alpha ?: 0

                listOf(
                    green shl 8 or red,
                    alpha shl 8 or blue
                ).map { (it.toDouble() / a - b).toFloat() }

            }
        }

    }
}