package com.kylecorry.trail_sense.tools.map.infrastructure

import android.content.Context
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.bitmaps.BitmapUtils.use
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.ImageRegionLoader
import com.kylecorry.trail_sense.shared.andromeda_temp.get
import com.kylecorry.trail_sense.shared.andromeda_temp.getPixels
import com.kylecorry.trail_sense.shared.data.AssetInputStreamable
import com.kylecorry.trail_sense.shared.data.EncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.data.SingleImageReader

object LandModel {

    // Image data source
    private val size = Size(3800, 1900)
    private val precision = 4
    private val offset = 0.5f

    private val source = GeographicImageSource(
        EncodedDataImageReader(
            SingleImageReader(size, AssetInputStreamable("land.webp")),
            decoder = EncodedDataImageReader.scaledDecoder(1.0, 0.0, false),
            maxChannels = 3,
        ),
        precision = precision,
        interpolationOrder = 0
    )

    suspend fun getCoastalLocations(
        context: Context,
        bounds: CoordinateBounds
    ): List<Coordinate> = onIO {
        val minWaterNeighbors = 1
        val minLandNeighbors = 1

        val fileSystem = AssetFileSystem(context)
        val coastalLocations = mutableListOf<Coordinate>()

        val topLeft = source.getPixel(Coordinate(bounds.north, bounds.west))
        val bottomRight = source.getPixel(Coordinate(bounds.south, bounds.east))

        val right = if (topLeft.x > bottomRight.x) {
            bottomRight.x + source.imageSize.width
        } else {
            bottomRight.x
        }

        // Add padding for neighbors
        val rect = Rect(
            (topLeft.x.toInt() - 1),
            (topLeft.y.toInt() - 1),
            (right.toInt() + 1),
            (bottomRight.y.toInt() + 1)
        )

        fileSystem.stream("land.webp").use { stream ->
            ImageRegionLoader.decodeBitmapRegionWrapped(stream, rect, size, wrap = true)
                .use {
                    val pixels = getPixels()
                    val w = width
                    val h = height

                    for (y in 1 until h - 1) {
                        for (x in 1 until w - 1) {
                            val color = pixels.get(x, y, w)
                            if (!isWater(color)) {
                                continue
                            }

                            var landNeighbors = 0
                            var waterNeighbors = 0

                            // Check 8 neighbors
                            for (dy in -1..1) {
                                for (dx in -1..1) {
                                    if (dx == 0 && dy == 0) {
                                        continue
                                    }
                                    val neighborColor = pixels.get(x + dx, y + dy, w)
                                    if (!isWater(neighborColor)) {
                                        landNeighbors++
                                    } else {
                                        waterNeighbors++
                                    }
                                }
                            }

                            if (landNeighbors >= minLandNeighbors && waterNeighbors >= minWaterNeighbors) {
                                val globalX =
                                    SolMath.wrap(
                                        rect.left + x.toFloat() + offset,
                                        0f,
                                        size.width.toFloat()
                                    )
                                val globalY =
                                    (rect.top + y + offset)
                                        .coerceIn(0f, size.height.toFloat() - 1f)
                                coastalLocations.add(
                                    source.getLocation(
                                        PixelCoordinate(
                                            globalX,
                                            globalY
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
        }
        coastalLocations
    }

    private fun isWater(color: Int): Boolean {
        return color.red == 0 && color.green == 0 && color.blue == 0
    }

}
