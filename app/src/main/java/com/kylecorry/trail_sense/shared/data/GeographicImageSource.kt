package com.kylecorry.trail_sense.shared.data

import android.graphics.Rect
import com.kylecorry.andromeda.bitmaps.FloatBitmap
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import kotlin.math.floor

class GeographicImageSource(
    private val reader: DataImageReader,
    val bounds: CoordinateBounds = CoordinateBounds.world,
    private val precision: Int = 2,
    private val valuePixelOffset: Float = 0f,
    // TODO: All of these should be hidden from the geographic image source
    private val interpolationOrder: Int = 1
) {
    val imageSize = reader.getSize()
    private val horizontalRes = bounds.widthDegrees() / imageSize.width
    private val verticalRes = bounds.heightDegrees() / imageSize.height
    private val west = bounds.west + horizontalRes * valuePixelOffset
    private val north = bounds.north - verticalRes * valuePixelOffset

    private fun getX(longitude: Double): Double {
        return (longitude - west) / horizontalRes
    }

    private fun getY(latitude: Double): Double {
        return (north - latitude) / verticalRes
    }

    fun getPixel(location: Coordinate, clamp: Boolean = true): PixelCoordinate {
        var x = getX(location.longitude)
        var y = getY(location.latitude)

        if (x.isNaN()) {
            x = 0.0
        }

        if (y.isNaN()) {
            y = 0.0
        }

        if (!clamp) {
            return PixelCoordinate(
                x.roundPlaces(precision).toFloat(),
                y.roundPlaces(precision).toFloat()
            )
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

    fun getLocation(pixel: PixelCoordinate): Coordinate {
        val imageSize = reader.getSize()
        val horizontalRes = bounds.widthDegrees() / imageSize.width
        val verticalRes = bounds.heightDegrees() / imageSize.height

        val longitude = (pixel.x + valuePixelOffset) * horizontalRes + bounds.west
        val latitude = bounds.north - (pixel.y + valuePixelOffset) * verticalRes

        return Coordinate(
            latitude.roundPlaces(precision),
            Coordinate.toLongitude(longitude.roundPlaces(precision))
        )
    }

    suspend fun read(
        location: Coordinate,
        neighborSources: List<GeographicImageSource> = emptyList()
    ): FloatArray = onIO {
        val output = FloatBitmap(1, 1, reader.channels)
        read(
            doubleArrayOf(location.latitude),
            doubleArrayOf(location.longitude),
            output,
            neighborSources
        )
        output.data
    }

    suspend fun read(pixel: PixelCoordinate): FloatArray = onIO {
        read(listOf(pixel)).first().second
    }

    suspend fun read(
        latitudes: DoubleArray,
        longitudes: DoubleArray,
        output: FloatBitmap,
        neighborSources: List<GeographicImageSource> = emptyList()
    ) = onIO {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        // Pre-calculate Y pixels
        val yPixels = FloatArray(latitudes.size)
        val yInside = BooleanArray(latitudes.size)
        for (i in latitudes.indices) {
            val lat = latitudes[i]
            if (!bounds.containsLatitude(lat)) {
                yInside[i] = false
                continue
            }
            val y = getY(lat).toFloat()
            yPixels[i] = y
            yInside[i] = true
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }

        if (minY == Float.MAX_VALUE) {
            return@onIO
        }

        // Pre-calculate X pixels
        val xPixels = FloatArray(longitudes.size)
        val xInside = BooleanArray(longitudes.size)
        for (i in longitudes.indices) {
            val lon = longitudes[i]
            if (!bounds.containsLongitude(lon)) {
                xInside[i] = false
                continue
            }
            val x = getX(lon).toFloat()
            xPixels[i] = x
            xInside[i] = true
            if (x < minX) minX = x
            if (x > maxX) maxX = x
        }

        if (minX == Float.MAX_VALUE) {
            return@onIO
        }

        // Read the region and add padding for interpolation
        val left = (floor(minX).toInt() - interpolationOrder).coerceAtLeast(0)
        val top = (floor(minY).toInt() - interpolationOrder).coerceAtLeast(0)
        val right = (floor(maxX).toInt() + 1 + interpolationOrder).coerceAtMost(imageSize.width)
        val bottom = (floor(maxY).toInt() + 1 + interpolationOrder).coerceAtMost(imageSize.height)

        if (right <= left || bottom <= top) {
            return@onIO
        }

        val rect = Rect(left, top, right, bottom)
        val (pixelGrid, hasData) = reader.getRegion(rect) ?: return@onIO

        if (!hasData) {
            return@onIO
        }

        val needsCrossBoundary = neighborSources.isNotEmpty() && interpolationOrder > 0 && (
                left == 0 || top == 0 ||
                        right == imageSize.width || bottom == imageSize.height
                )

        val pixelProvider = if (needsCrossBoundary) {
            val actualNeighbors = neighborSources.filter { it !== this@GeographicImageSource }
            if (actualNeighbors.isNotEmpty()) {
                CrossBoundaryPixelProvider(
                    this@GeographicImageSource,
                    pixelGrid,
                    rect,
                    actualNeighbors
                )
            } else null
        } else null

        val interpolator = FloatBitmapInterpolator(interpolationOrder, pixelProvider)
        val outputData = output.data
        val outputWidth = output.width
        val channels = output.channels

        // Interpolate
        for (yIdx in latitudes.indices) {
            if (!yInside[yIdx]) continue
            val y = yPixels[yIdx]

            for (xIdx in longitudes.indices) {
                if (!xInside[xIdx]) continue
                val x = xPixels[xIdx]

                val outputOffset = (yIdx * outputWidth + xIdx) * channels
                interpolator.getValue(pixelGrid, rect, x, y, outputData, outputOffset)
            }
        }
    }

    suspend fun read(pixels: List<PixelCoordinate>): List<Pair<PixelCoordinate, FloatArray>> =
        onIO {
            // TODO: Extract this and make the size configurable (maybe make it part of the region loader?)
            // Divide the pixels into subregions of at most 255x255 pixels in the original image (using x, y)
            val regions = partitionPixels(pixels)
            // TODO: Move the interpolation out of this class?
            val results = mutableListOf<Pair<PixelCoordinate, FloatArray>>()
            val interpolator = FloatBitmapInterpolator(interpolationOrder)
            for (region in regions) {
                val rect = getRect(region, interpolationOrder) ?: continue
                val (pixelGrid, hasData) = reader.getRegion(rect) ?: continue
                val empty = FloatArray(pixelGrid.channels)
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

    suspend fun getRegion(rect: Rect): Pair<FloatBitmap, Boolean>? = onIO {
        reader.getRegion(rect)
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
        val imageSize = reader.getSize()

        val left = (floor(minX).toInt() - interpolationOrder).coerceAtLeast(0)
        val top = (floor(minY).toInt() - interpolationOrder).coerceAtLeast(0)
        val right = (floor(maxX).toInt() + 1 + interpolationOrder).coerceAtMost(imageSize.width)
        val bottom = (floor(maxY).toInt() + 1 + interpolationOrder).coerceAtMost(imageSize.height)

        if (right <= left || bottom <= top) {
            return null
        }

        return Rect(left, top, right, bottom)
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