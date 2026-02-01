package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.andromeda.bitmaps.FloatBitmap
import com.kylecorry.andromeda.bitmaps.operations.Convert
import com.kylecorry.andromeda.bitmaps.operations.applyOperations
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.CropTile
import com.kylecorry.trail_sense.shared.andromeda_temp.getMultiplesBetween2
import com.kylecorry.trail_sense.shared.andromeda_temp.set

object TileImageUtils {
    fun parallelGridEvaluation(
        getValue: (latitude: Double, longitude: Double) -> Float
    ): suspend (latitudes: DoubleArray, longitudes: DoubleArray) -> FloatBitmap {
        return { latitudes: DoubleArray, longitudes: DoubleArray ->
            val bitmap = FloatBitmap(longitudes.size, latitudes.size, 1)
            Parallel.forEach(latitudes.indices.toList()) { y ->
                val latitude = latitudes[y]
                for (x in longitudes.indices) {
                    bitmap.set(x, y, 0, getValue(latitude, longitudes[x]))
                }
            }
            bitmap
        }
    }

    /**
     * Total lookup points = (samples + 4)^2
     * If less than 2 samples are used, results will be degraded
     */
    fun interpolatedGridEvaluation(
        samples: Int,
        getValue: (latitude: Double, longitude: Double) -> Float
    ): suspend (latitudes: DoubleArray, longitudes: DoubleArray) -> FloatBitmap {
        val expandBy = 2
        return { latitudes: DoubleArray, longitudes: DoubleArray ->
            val smallLatitudes = resample(latitudes.first(), latitudes.last(), samples, expandBy)
            val unwrappedLongitudes = unwrapLongitudes(longitudes)
            val smallLongitudes =
                resample(unwrappedLongitudes.first(), unwrappedLongitudes.last(), samples, expandBy)
            val smallBitmap = FloatBitmap(smallLongitudes.size, smallLatitudes.size, 1)
            Parallel.forEach(smallLatitudes.indices.toList()) { y ->
                val latitude = smallLatitudes[y]
                for (x in smallLongitudes.indices) {
                    smallBitmap.set(
                        x,
                        y,
                        0,
                        getValue(latitude, Coordinate.toLongitude(smallLongitudes[x]))
                    )
                }
            }


            smallBitmap.upscale(longitudes.size, latitudes.size, expandBy)
        }
    }

    fun getRequiredResolution(tile: Tile, samples: Int): Double {
        val width = TileMath.getTile(0.0, 0.0, tile.z).getBounds().widthDegrees()
        return width / samples
    }

    private fun resample(start: Double, end: Double, count: Int, expandBy: Int = 0): DoubleArray {
        val array = DoubleArray(count + expandBy * 2)
        if (array.isEmpty()) {
            return array
        }

        val baseCount = if (count > 1) count - 1 else 1
        val resolution = (end - start) / baseCount
        val spanStart = start - resolution * expandBy
        val spanEnd = end + resolution * expandBy
        val denominator = if (array.size > 1) array.size - 1 else 1
        for (i in array.indices) {
            val percent = i / denominator.toDouble()
            array[i] = SolMath.lerp(percent, spanStart, spanEnd)
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

    suspend fun getSampledImage(
        bounds: CoordinateBounds,
        resolution: Double,
        size: Size,
        config: Bitmap.Config = Bitmap.Config.RGB_565,
        padding: Int = 0,
        normalizeLongitudes: Boolean = true,
        useBilinearInterpolation: Boolean = true,
        smoothPixelEdges: Boolean = false,
        getValues: suspend (latitudes: DoubleArray, longitudes: DoubleArray) -> FloatBitmap,
        getColor: suspend (x: Int, y: Int, getValue: (x: Int, y: Int) -> Float) -> Int
    ): Bitmap = onDefault {
        val expandBy = 1
        val latitudes = Interpolation.getMultiplesBetween2(
            bounds.south - resolution * (expandBy + padding),
            bounds.north + resolution * (expandBy + padding),
            resolution
        )

        val longitudes = Interpolation.getMultiplesBetween2(
            bounds.west - resolution * (expandBy + padding),
            (if (bounds.west < bounds.east) bounds.east else bounds.east + 360) + resolution * (expandBy + padding),
            resolution
        )

        if (normalizeLongitudes) {
            for (i in longitudes.indices) {
                longitudes[i] = Coordinate.toLongitude(longitudes[i])
            }
        }

        val values = getValues(latitudes, longitudes)
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
                size,
                useBilinearInterpolation,
                smoothPixelEdges
            ),
            Convert(config)
        )
    }
}