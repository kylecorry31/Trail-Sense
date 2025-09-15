package com.kylecorry.trail_sense.shared.andromeda_temp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.blue
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.math.SolMath.wrap
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import java.io.InputStream
import kotlin.math.roundToInt

object GeographicImageUtils {

    // TODO: Redo this
    suspend fun getNearestPixelOfAsset(
        source: GeographicImageSource,
        context: Context,
        location: Coordinate,
        imagePath: String,
        searchSize: Int = 5,
        hasValue: (Int) -> Boolean = { it.red > 0 || it.green > 0 || it.blue > 0 },
        hasMappedValue: (FloatArray) -> Boolean = { it.dropLast(1).any { it > 0 } },
    ): PixelCoordinate? {
        val actualPixel = source.getPixel(location)
        val sourceValue = source.read(actualPixel)
        if (hasMappedValue(sourceValue)) {
            return actualPixel
        }

        val fileSystem = AssetFileSystem(context)
        fileSystem.stream(imagePath).use { stream ->
            var bitmap: Bitmap? = null
            try {
                bitmap = loadRegion(stream, actualPixel, searchSize, source.imageSize)

                // Get the nearest non-zero pixel, wrapping around the image
                val x = searchSize
                val y = searchSize

                // Search in a grid pattern
                for (i in 1 until searchSize) {
                    val topY = y - i
                    val bottomY = y + i
                    val leftX = (x - i)
                    val rightX = (x + i)

                    val hits = mutableListOf<PixelCoordinate>()

                    // Check the top and bottom rows
                    for (j in leftX..rightX) {
                        if (hasValue(bitmap[j, topY])) {
                            hits.add(PixelCoordinate(j.toFloat(), topY.toFloat()))
                        }
                        if (hasValue(bitmap[j, bottomY])) {
                            hits.add(PixelCoordinate(j.toFloat(), bottomY.toFloat()))
                        }
                    }

                    // Check the left and right columns
                    for (j in topY..bottomY) {
                        if (hasValue(bitmap[leftX, j])) {
                            hits.add(PixelCoordinate(leftX.toFloat(), j.toFloat()))
                        }
                        if (hasValue(bitmap[rightX, j])) {
                            hits.add(PixelCoordinate(rightX.toFloat(), j.toFloat()))
                        }
                    }

                    if (hits.isNotEmpty()) {
                        val globalHits = hits.map {
                            // Only x is wrapped
                            val globalX =
                                wrap(
                                    actualPixel.x + it.x - searchSize,
                                    0f,
                                    source.imageSize.width.toFloat()
                                )
                            val globalY = actualPixel.y + it.y - searchSize

                            PixelCoordinate(globalX, globalY)
                        }
                        return globalHits.minByOrNull { it.distanceTo(actualPixel) } ?: actualPixel
                    }
                }
            } finally {
                bitmap?.recycle()
            }
        }

        return null
    }

    private fun loadRegion(
        stream: InputStream,
        center: PixelCoordinate,
        size: Int,
        fullImageSize: Size
    ): Bitmap {
        val cx = center.x.roundToInt()
        val cy = center.y.roundToInt()

        // Step 1: Calculate the region bounds
        val left = cx - size
        val top = cy - size
        val right = cx + size + 1
        val bottom = cy + size + 1


        // Step 2: Load as much of the region as possible
        val rect = Rect(left, top, right, bottom)

        return ImageRegionLoader.decodeBitmapRegionWrapped(stream, rect, fullImageSize, wrap = true)
    }

}