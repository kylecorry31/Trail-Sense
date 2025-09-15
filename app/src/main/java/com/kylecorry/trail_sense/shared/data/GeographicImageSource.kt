package com.kylecorry.trail_sense.shared.data

import android.graphics.Rect
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.heightDegrees
import com.kylecorry.trail_sense.shared.andromeda_temp.widthDegrees
import kotlin.math.floor

typealias FloatBitmap = Array<Array<FloatArray>>

class GeographicImageSource(
    private val reader: DataImageReader,
    private val bounds: CoordinateBounds = CoordinateBounds.world,
    private val precision: Int = 2,
    private val valuePixelOffset: Float = 0f,
    // TODO: All of these should be hidden from the geographic image source
    private val interpolationOrder: Int = 1
) {
    val imageSize = reader.getSize()

    fun getPixel(location: Coordinate): PixelCoordinate {
        val imageSize = reader.getSize()
        var x: Double
        var y: Double

        val horizontalRes = bounds.widthDegrees() / imageSize.width
        val verticalRes = bounds.heightDegrees() / imageSize.height
        x =
            (location.longitude - (bounds.west + horizontalRes * valuePixelOffset)) / horizontalRes
        y = ((bounds.north - verticalRes * valuePixelOffset) - location.latitude) / verticalRes

        if (x.isNaN()) {
            x = 0.0
        }

        if (y.isNaN()) {
            y = 0.0
        }
        return PixelCoordinate(
            x.roundPlaces(precision).toFloat().coerceIn(
                -valuePixelOffset,
                imageSize.width.toFloat() - 1f + valuePixelOffset
            ),
            y.roundPlaces(precision).toFloat().coerceIn(
                -valuePixelOffset,
                imageSize.height.toFloat() - 1f + valuePixelOffset
            )
        )
    }

    suspend fun read(location: Coordinate): FloatArray = onIO {
        read(getPixel(location))
    }

    suspend fun read(pixel: PixelCoordinate): FloatArray = onIO {
        read(listOf(pixel)).first().second
    }

    suspend fun read(pixels: List<PixelCoordinate>): List<Pair<PixelCoordinate, FloatArray>> =
        onIO {
            // TODO: Extract this and make the size configurable (maybe make it part of the region loader?)
            // Divide the pixels into subregions of at most 255x255 pixels in the original image (using x, y)
            val regions = partitionPixels(pixels)
            // TODO: Move the interpolation out of this class?
            val results = mutableListOf<Pair<PixelCoordinate, FloatArray>>()
            for (region in regions) {
                val rect = getRect(region, interpolationOrder) ?: continue
                val (pixelGrid, hasData) = reader.getRegion(rect) ?: continue
                val interpolator = FloatBitmapInterpolator(interpolationOrder)
                val empty = FloatArray(pixelGrid[0][0].size)
                for (pixel in region) {
                    var interpolated = empty
                    if (hasData) {
                        interpolated = interpolator.getValue(pixelGrid, rect, pixel.x, pixel.y)
                    }
                    results.add(pixel to interpolated)
                }
            }
            results
        }

    fun contains(location: Coordinate): Boolean {
        return bounds.contains(location)
    }

    private fun getRect(pixels: List<PixelCoordinate>, interpolationOrder: Int): Rect? {
        if (pixels.isEmpty()) {
            return null
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
        return Rect(
            floor(minX).toInt() - interpolationOrder,
            floor(minY).toInt() - interpolationOrder,
            floor(maxX).toInt() + 1 + interpolationOrder,
            floor(maxY).toInt() + 1 + interpolationOrder
        )
    }

    private fun partitionPixels(
        pixels: List<PixelCoordinate>,
        tileSize: Int = 255
    ): List<List<PixelCoordinate>> {
        val regions = mutableMapOf<Pair<Int, Int>, MutableList<PixelCoordinate>>()
        for (pixel in pixels) {
            val regionX = (pixel.x / tileSize).toInt()
            val regionY = (pixel.y / tileSize).toInt()
            val regionKey = Pair(regionX, regionY)
            if (regionKey !in regions) {
                regions[regionKey] = mutableListOf()
            }
            regions[regionKey]!!.add(pixel)
        }
        return regions.values.toList()
    }
}