package com.kylecorry.trail_sense.shared.data

import android.content.Context
import android.util.Size
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

class GeographicImageSource(
    val imageSize: Size,
    private val bounds: CoordinateBounds = CoordinateBounds.world,
    private val latitudePixelsPerDegree: Double = ((imageSize.height - 1) / abs(bounds.north - bounds.south)),
    private val longitudePixelsPerDegree: Double = ((imageSize.width - 1) / abs(bounds.east - bounds.west)),
    private val precision: Int = 2,
    private val interpolate: Boolean = true,
    private val include0ValuesInInterpolation: Boolean = true,
    private val interpolationOrder: Int = 1,
    private val valuePixelOffset: Float = 0f,
    private val maxChannels: Int? = null,
    private val decoder: (Int?) -> List<Float> = { listOf(it?.toFloat() ?: 0f) }
) {

    private val reader = ImagePixelReader2(imageSize, lookupOrder = interpolationOrder, returnAllPixels = true)

    fun getPixel(location: Coordinate): PixelCoordinate {
        var x: Double
        var y: Double

        if (!SolMath.isZero(valuePixelOffset)) {
            val horizontalRes = abs(bounds.west - bounds.east) / imageSize.width
            val verticalRes = abs(bounds.north - bounds.south) / imageSize.height
            x =
                (location.longitude - (bounds.west + horizontalRes * valuePixelOffset)) / horizontalRes
            y = ((bounds.north - verticalRes * valuePixelOffset) - location.latitude) / verticalRes
        } else {
            x = (location.longitude - bounds.west) * longitudePixelsPerDegree
            y = (bounds.north - location.latitude) * latitudePixelsPerDegree
        }

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

    suspend fun read(stream: InputStream, location: Coordinate): List<Float> = onIO {
        val pixel = getPixel(location)
        read(stream, pixel)
    }

    suspend fun read(
        streamProvider: suspend () -> InputStream,
        pixels: List<PixelCoordinate>
    ): List<Pair<PixelCoordinate, List<Float>>> = onIO {
        // Divide the pixels into subregions of at most 255x255 pixels in the original image (using x, y)
        val regions = mutableMapOf<Pair<Int, Int>, MutableList<PixelCoordinate>>()
        for (pixel in pixels) {
            val regionX = (pixel.x / 255).toInt()
            val regionY = (pixel.y / 255).toInt()
            val regionKey = Pair(regionX, regionY)
            if (regionKey !in regions) {
                regions[regionKey] = mutableListOf()
            }
            regions[regionKey]!!.add(pixel)
        }
        val results = mutableListOf<Pair<PixelCoordinate, List<Float>>>()
        val interpolators = listOfNotNull(
            if (interpolate && interpolationOrder == 2) BicubicInterpolator<Float>() else null,
            if (interpolate) BilinearInterpolator<Float>() else null,
            NearestInterpolator<Float>()
        )
        for (region in regions.values) {
            // TODO: Get the result back from the reader as a grid
            val pixels = reader.getAllPixels(streamProvider(), region, true)
            val decoded = pixels.map { it to decoder(it.value) }
            var channels = decoded.firstOrNull()?.second?.size ?: 0
            if (maxChannels != null) {
                channels = minOf(channels, maxChannels)
            }

            var minX = Int.MAX_VALUE
            var minY = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            var maxY = Int.MIN_VALUE
            for (pair in decoded) {
                val pixelResult = pair.first

                if (pixelResult.x < minX) {
                    minX = pixelResult.x
                }
                if (pixelResult.y < minY) {
                    minY = pixelResult.y
                }
                if (pixelResult.x > maxX) {
                    maxX = pixelResult.x
                }
                if (pixelResult.y > maxY) {
                    maxY = pixelResult.y
                }
            }

            val width = (maxX - minX) + 1
            val height = (maxY - minY) + 1

            val pixelGrid = List(channels) {
                List(height) {
                    MutableList(width) { Float.NaN }
                }
            }

            var isAllNaN = true
            (0 until channels).forEach { channel ->
                decoded.forEach { pair ->
                    val pixelResult = pair.first
                    val values = pair.second

                    if (!include0ValuesInInterpolation && SolMath.isZero(values[channel])) {
                        return@forEach
                    }

                    val x = (pixelResult.x - minX)
                    val y = (pixelResult.y - minY)

                    if (x in 0 until width && y in 0 until height) {
                        val value = values[channel]
                        pixelGrid[channel][y][x] = value
                        isAllNaN = false
                    }
                }
            }

            for (pixel in region) {
                val interpolated = mutableListOf<Float>()
                for (i in 0 until channels) {
                    if (isAllNaN) {
                        interpolated.add(0f)
                        continue
                    }
                    val values = pixelGrid[i]
                    val localPixel = PixelCoordinate(
                        pixel.x - minX,
                        pixel.y - minY
                    )
                    interpolated.add(interpolators.firstNotNullOfOrNull {
                        it.interpolate(localPixel, values)
                    } ?: 0f)
                }
                results.add(pixel to interpolated)
            }
        }
        results
    }

    suspend fun read(stream: InputStream, pixel: PixelCoordinate): List<Float> = onIO {
        read({ stream }, listOf(pixel)).first().second
    }

    suspend fun read(context: Context, filename: String, location: Coordinate): List<Float> = onIO {
        val fileSystem = AssetFileSystem(context)
        read(fileSystem.stream(filename), location)
    }

    suspend fun read(context: Context, filename: String, pixel: PixelCoordinate): List<Float> =
        onIO {
            val fileSystem = AssetFileSystem(context)
            read(fileSystem.stream(filename), pixel)
        }

    fun contains(location: Coordinate): Boolean {
        return bounds.contains(location)
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

interface PixelInterpolator<T : Number> {
    fun interpolate(
        pixel: PixelCoordinate,
        pixels: List<List<T>>
    ): Float?
}

class NearestInterpolator<T : Number> : PixelInterpolator<T> {
    override fun interpolate(
        pixel: PixelCoordinate,
        pixels: List<List<T>>
    ): Float? {
        // TODO: Extract this
        if (pixels.isEmpty()) {
            return null
        }
        val height = pixels.size
        val width = pixels.first().size
        if (width == 0) {
            return null
        }

        val x = pixel.x
        val y = pixel.y
        val xInt = x.roundToInt()
        val yInt = y.roundToInt()

        var bestValue: T? = null
        var bestDist = Float.MAX_VALUE

        val maxRadius = maxOf(width, height)

        fun process(cx: Int, cy: Int) {
            if (cx < 0 || cy < 0 || cy >= height || cx >= width) {
                return
            }
            val value = pixels[cy][cx]
            if (value.toFloat().isNaN()) {
                return
            }
            val dx = (cx - x)
            val dy = (cy - y)
            val dist = dx * dx + dy * dy
            if (dist < bestDist) {
                bestDist = dist
                bestValue = value
            }
        }

        for (r in 0..maxRadius) {
            if (r == 0) {
                process(xInt, yInt)
            } else {
                val left = xInt - r
                val right = xInt + r
                val top = yInt - r
                val bottom = yInt + r

                // Top & bottom
                for (cx in left..right) {
                    process(cx, top)
                    process(cx, bottom)
                }
                // Left & right
                for (cy in (top + 1) until bottom) {
                    process(left, cy)
                    process(right, cy)
                }
            }
            if (bestValue != null) {
                return bestValue.toFloat()
            }
        }

        return bestValue?.toFloat()
    }
}

class BilinearInterpolator<T : Number> : PixelInterpolator<T> {
    override fun interpolate(
        pixel: PixelCoordinate,
        pixels: List<List<T>>
    ): Float? {
        // Find the 4 corners
        val xFloor = pixel.x.toInt()
        val yFloor = pixel.y.toInt()
        val xCeil = xFloor + 1
        val yCeil = yFloor + 1
        val x1y1 = pixels.getOrNull(yFloor)?.getOrNull(xFloor)
        val x1y2 = pixels.getOrNull(yCeil)?.getOrNull(xFloor)
        val x2y1 = pixels.getOrNull(yFloor)?.getOrNull(xCeil)
        val x2y2 = pixels.getOrNull(yCeil)?.getOrNull(xCeil)

        // Not enough values to interpolate
        if (x1y1 == null || x1y2 == null || x2y1 == null || x2y2 == null ||
            x1y1.toFloat().isNaN() ||
            x1y2.toFloat().isNaN() ||
            x2y1.toFloat().isNaN() ||
            x2y2.toFloat().isNaN()
        ) {
            return null
        }

        // Interpolate
        val x1y1Weight = (xCeil - pixel.x) * (yCeil - pixel.y)
        val x1y2Weight = (xCeil - pixel.x) * (pixel.y - yFloor)
        val x2y1Weight = (pixel.x - xFloor) * (yCeil - pixel.y)
        val x2y2Weight = (pixel.x - xFloor) * (pixel.y - yFloor)
        return x1y1.toFloat() * x1y1Weight + x1y2.toFloat() * x1y2Weight + x2y1.toFloat() * x2y1Weight + x2y2.toFloat() * x2y2Weight
    }
}

class BicubicInterpolator<T : Number> : PixelInterpolator<T> {
    private fun cubic(t: Float): Float {
        val tAbs = abs(t)
        return when {
            tAbs <= 1f -> SolMath.polynomial(tAbs.toDouble(), 1.0, 0.0, -2.5, 1.5).toFloat()
            tAbs <= 2f -> SolMath.polynomial(tAbs.toDouble(), 2.0, -4.0, 2.5, -0.5).toFloat()
            else -> 0f
        }
    }

    override fun interpolate(
        pixel: PixelCoordinate,
        pixels: List<List<T>>
    ): Float? {
        val x = pixel.x
        val y = pixel.y

        val xInt = floor(x).toInt()
        val yInt = floor(y).toInt()

        val fx = x - xInt
        val fy = y - yInt

        val rowVals = mutableListOf<Float>()
        for (i in 0 until 4) {
            var value = 0f
            for (j in 0 until 4) {
                val currentX = xInt + j - 1
                val currentY = yInt + i - 1
                val pixelValue = pixels.getOrNull(currentY)?.getOrNull(currentX) ?: return null

                if (pixelValue.toFloat().isNaN()) {
                    return null
                }

                value += pixelValue.toFloat() * cubic(fx - (j - 1).toFloat())
            }
            rowVals.add(value)
        }

        var result = 0f
        for (i in 0 until 4) {
            result += rowVals[i] * cubic(fy - (i - 1).toFloat())
        }

        return result
    }
}