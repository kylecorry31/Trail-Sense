package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.andromeda.bitmaps.FloatBitmap
import com.kylecorry.andromeda.bitmaps.operations.Conditional
import com.kylecorry.andromeda.bitmaps.operations.Convert
import com.kylecorry.andromeda.bitmaps.operations.Resize
import com.kylecorry.andromeda.bitmaps.operations.applyOperations
import com.kylecorry.andromeda.bitmaps.operations.set
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.CropTile
import com.kylecorry.trail_sense.shared.andromeda_temp.PixelPreservationUpscale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt


interface CoordinateGridValueProvider {
    suspend fun getValues(latitudes: DoubleArray, longitudes: DoubleArray): FloatBitmap
}

class ParallelCoordinateGridValueProvider(
    private val getValue: (latitude: Double, longitude: Double) -> Float
) : CoordinateGridValueProvider {
    override suspend fun getValues(
        latitudes: DoubleArray,
        longitudes: DoubleArray
    ): FloatBitmap {
        val bitmap = FloatBitmap(longitudes.size, latitudes.size, 1)
        Parallel.forEach(latitudes.indices.toList()) { y ->
            val latitude = latitudes[y]
            Parallel.forEach(longitudes.indices.toList()) { x ->
                bitmap.set(x, y, 0, getValue(latitude, Coordinate.toLongitude(longitudes[x])))
            }
        }
        return bitmap
    }
}

class InterpolatedGridValueProvider(
    private val samples: Int,
    private val valueProvider: CoordinateGridValueProvider
) : CoordinateGridValueProvider {
    private val expandBy = 2

    override suspend fun getValues(
        latitudes: DoubleArray,
        longitudes: DoubleArray
    ): FloatBitmap {
        val unwrappedLongitudes = unwrapLongitudes(longitudes)
        val step =
            (unwrappedLongitudes.last() - unwrappedLongitudes.first()) / (if (samples > 1) samples - 1 else 1)

        val sampledLatitudes = resample(latitudes.first(), latitudes.last(), step)
        val sampledLongitudes =
            resample(unwrappedLongitudes.first(), unwrappedLongitudes.last(), step)
        val sampledBitmap = valueProvider.getValues(sampledLatitudes, sampledLongitudes)

        val startX =
            ((unwrappedLongitudes.first() - sampledLongitudes.first()) / step).toFloat()
        val endX = ((unwrappedLongitudes.last() - sampledLongitudes.first()) / step).toFloat()
        val startY = ((latitudes.first() - sampledLatitudes.first()) / step).toFloat()
        val endY = ((latitudes.last() - sampledLatitudes.first()) / step).toFloat()

        return sampledBitmap.upscale(longitudes.size, latitudes.size, startX, endX, startY, endY)
    }

    private fun resample(start: Double, end: Double, step: Double): DoubleArray {
        val spanStart = floor((start - step * expandBy) / step) * step
        val spanEnd = ceil((end + step * expandBy) / step) * step
        val count = ((spanEnd - spanStart) / step).roundToInt() + 1
        val array = DoubleArray(count)
        for (i in array.indices) {
            array[i] = spanStart + i * step
        }
        return array
    }

    private fun unwrapLongitudes(longitudes: DoubleArray): DoubleArray {
        if (longitudes.isEmpty()) {
            return longitudes
        }

        val unwrapped = DoubleArray(longitudes.size)
        var offset = 0.0
        unwrapped[0] = longitudes[0]
        for (i in 1 until longitudes.size) {
            val current = longitudes[i]
            val previous = longitudes[i - 1]
            val delta = current - previous
            if (delta < -180) {
                offset += 360.0
            } else if (delta > 180) {
                offset -= 360.0
            }
            unwrapped[i] = current + offset
        }
        return unwrapped
    }

}


object TileImageUtils {

    fun getRequiredResolution(tile: Tile, samples: Int): Double {
        val width = TileMath.getTile(0.0, 0.0, tile.z).getBounds().widthDegrees()
        return width / samples
    }

    suspend fun getSampledImage(
        bounds: CoordinateBounds,
        resolution: Double,
        size: Size,
        config: Bitmap.Config = Bitmap.Config.RGB_565,
        padding: Int = 0,
        normalizeLongitudes: Boolean = true,
        interpolate: Boolean = true,
        smoothPixelEdges: Boolean = false,
        valueProvider: CoordinateGridValueProvider,
        getColor: suspend (x: Int, y: Int, getValue: (x: Int, y: Int) -> Float) -> Int
    ): Bitmap = onDefault {
        val expandBy = 1
        val latitudes = Interpolation.getMultiplesBetween(
            bounds.south - resolution * (expandBy + padding),
            bounds.north + resolution * (expandBy + padding),
            resolution
        )

        val longitudes = Interpolation.getMultiplesBetween(
            bounds.west - resolution * (expandBy + padding),
            (if (bounds.west < bounds.east) bounds.east else bounds.east + 360) + resolution * (expandBy + padding),
            resolution
        )

        if (normalizeLongitudes) {
            for (i in longitudes.indices) {
                longitudes[i] = Coordinate.toLongitude(longitudes[i])
            }
        }

        val values = valueProvider.getValues(latitudes, longitudes)
        val width = values.width - expandBy * 2
        val height = values.height - expandBy * 2
        val pixels = IntArray(width * height)

        val getValue = { x: Int, y: Int ->
            values.get(x, y, 0)
        }

        for (y in expandBy until values.height - expandBy) {
            for (x in expandBy until values.width - expandBy) {
                pixels.set(
                    x - expandBy,
                    height - 1 - (y - expandBy),
                    width,
                    getColor(x, y, getValue)
                )
            }
        }

        val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

        val south = latitudes[expandBy] - resolution / 2.0
        val north = latitudes[values.height - expandBy - 1] + resolution / 2.0
        val west = longitudes[expandBy] - resolution / 2.0
        val east = longitudes[values.width - expandBy - 1] + resolution / 2.0

        val imageBounds = CoordinateBounds(north, east, south, west)

        bitmap.applyOperations(
            CropTile(
                imageBounds,
                bounds,
                size
            ) {
                listOf(
                    Conditional(smoothPixelEdges, PixelPreservationUpscale(it)),
                    Resize(it, true, interpolate)
                )
            },
            Convert(config)
        )
    }
}