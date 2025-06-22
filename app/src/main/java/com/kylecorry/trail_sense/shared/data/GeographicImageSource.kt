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
    private val decoder: (Int?) -> List<Float> = { listOf(it?.toFloat() ?: 0f) }
) {

    private val reader = ImagePixelReader2(imageSize, lookupOrder = interpolationOrder)

    fun getPixel(location: Coordinate): PixelCoordinate {
        var x = 0.0
        var y = 0.0

        if (!SolMath.isZero(valuePixelOffset)) {
            val res = abs(bounds.west - bounds.east) / imageSize.width
            x = (location.longitude - (bounds.west + res * valuePixelOffset)) / res
            y = ((bounds.north - res * valuePixelOffset) - location.latitude) / res
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
                0f,
                imageSize.width.toFloat() - 1f
            ),
            y.roundPlaces(precision).toFloat().coerceIn(
                0f,
                imageSize.height.toFloat() - 1f
            )
        )
    }

    suspend fun read(stream: InputStream, location: Coordinate): List<Float> = onIO {
        val pixel = getPixel(location)
        read(stream, pixel)
    }

    suspend fun read(stream: InputStream, pixel: PixelCoordinate): List<Float> = onIO {
        val pixels =
            reader.getPixels(stream, pixel.x, pixel.y, true)
        val decoded = pixels.map { it to decoder(it.value) }
        val channels = decoded.firstOrNull()?.second?.size ?: 0
        val interpolated = mutableListOf<Float>()
        val interpolators = listOfNotNull(
            if (interpolate && interpolationOrder == 2) BicubicInterpolator<Float>() else null,
            if (interpolate) BilinearInterpolator<Float>() else null,
            NearestInterpolator<Float>()
        )
        for (i in 0 until channels) {
            val values = decoded.map {
                PixelResult(
                    it.first.x,
                    it.first.y,
                    it.second[i],
                    it.first.order
                )
            }.filter {
                include0ValuesInInterpolation || !SolMath.isZero(it.value)
            }
            interpolated.add(interpolators.firstNotNullOfOrNull { it.interpolate(pixel, values) }
                ?: 0f)
        }
        interpolated
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
        pixels: List<PixelResult<T>>
    ): Float?
}

class NearestInterpolator<T : Number> : PixelInterpolator<T> {
    override fun interpolate(
        point: PixelCoordinate,
        values: List<PixelResult<T>>
    ): Float? {
        return values.minByOrNull { point.distanceTo(it.coordinate) }?.value?.toFloat()
    }
}

class BilinearInterpolator<T : Number> : PixelInterpolator<T> {
    override fun interpolate(
        point: PixelCoordinate,
        values: List<PixelResult<T>>
    ): Float? {
        // Find the 4 corners
        val xFloor = point.x.toInt()
        val yFloor = point.y.toInt()
        val xCeil = xFloor + 1
        val yCeil = yFloor + 1
        val x1y1 = values.firstOrNull { it.x == xFloor && it.y == yFloor }
        val x1y2 = values.firstOrNull { it.x == xFloor && it.y == yCeil }
        val x2y1 = values.firstOrNull { it.x == xCeil && it.y == yFloor }
        val x2y2 = values.firstOrNull { it.x == xCeil && it.y == yCeil }

        // Not enough values to interpolate
        if (x1y1 == null || x1y2 == null || x2y1 == null || x2y2 == null) {
            return null
        }

        // Interpolate
        val x1y1Weight = (x2y1.x - point.x) * (x1y2.y - point.y)
        val x1y2Weight = (x2y1.x - point.x) * (point.y - x1y1.y)
        val x2y1Weight = (point.x - x1y1.x) * (x1y2.y - point.y)
        val x2y2Weight = (point.x - x1y1.x) * (point.y - x1y1.y)
        return x1y1.value.toFloat() * x1y1Weight + x1y2.value.toFloat() * x1y2Weight + x2y1.value.toFloat() * x2y1Weight + x2y2.value.toFloat() * x2y2Weight
    }
}

class BicubicInterpolator<T : Number> : PixelInterpolator<T> {
    private fun cubicInterpolate(p: FloatArray, x: Float): Float {
        return p[1] + 0.5f * x * (p[2] - p[0] +
                x * (2f * p[0] - 5f * p[1] + 4f * p[2] - p[3] +
                x * (3f * (p[1] - p[2]) + p[3] - p[0])))
    }

    override fun interpolate(
        point: PixelCoordinate,
        values: List<PixelResult<T>>
    ): Float? {
        val x = point.x
        val y = point.y

        val xInt = x.toInt()
        val yInt = y.toInt()

        val resultRows = mutableListOf<Float>()

        for (m in -1..2) {
            val row = FloatArray(4)
            for (n in -1..2) {
                val px = xInt + n
                val py = yInt + m
                val value = values.firstOrNull { it.x == px && it.y == py }?.value?.toFloat()
                if (value == null) {
                    return null
                }
                row[n + 1] = value
            }
            resultRows.add(cubicInterpolate(row, x - xInt))
        }

        return cubicInterpolate(resultRows.toFloatArray(), y - yInt)
    }
}