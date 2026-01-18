package com.kylecorry.trail_sense.shared.data

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

interface PixelInterpolator {
    suspend fun interpolate(
        pixel: PixelCoordinate,
        getValue: suspend (x: Int, y: Int) -> Float?
    ): Float?
}

class NearestInterpolator(private val maxSearchRadius: Int = 10) : PixelInterpolator {
    override suspend fun interpolate(
        pixel: PixelCoordinate,
        getValue: suspend (x: Int, y: Int) -> Float?
    ): Float? {
        val x = pixel.x
        val y = pixel.y
        val xInt = x.roundToInt()
        val yInt = y.roundToInt()

        var bestValue: Float? = null
        var bestDist = Float.MAX_VALUE

        suspend fun process(cx: Int, cy: Int) {
            val value = getValue(cx, cy)
            if (value == null || value.isNaN()) {
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

        for (r in 0..maxSearchRadius) {
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
                return bestValue
            }
        }

        return bestValue
    }
}

class BilinearInterpolator : PixelInterpolator {
    override suspend fun interpolate(
        pixel: PixelCoordinate,
        getValue: suspend (x: Int, y: Int) -> Float?
    ): Float? {
        // Find the 4 corners
        val xFloor = pixel.x.toInt()
        val yFloor = pixel.y.toInt()
        val xCeil = xFloor + 1
        val yCeil = yFloor + 1
        val x1y1 = getValue(xFloor, yFloor)
        val x1y2 = getValue(xFloor, yCeil)
        val x2y1 = getValue(xCeil, yFloor)
        val x2y2 = getValue(xCeil, yCeil)

        // Not enough values to interpolate
        if (x1y1 == null || x1y2 == null || x2y1 == null || x2y2 == null ||
            x1y1.isNaN() ||
            x1y2.isNaN() ||
            x2y1.isNaN() ||
            x2y2.isNaN()
        ) {
            return null
        }

        // Interpolate
        val x1y1Weight = (xCeil - pixel.x) * (yCeil - pixel.y)
        val x1y2Weight = (xCeil - pixel.x) * (pixel.y - yFloor)
        val x2y1Weight = (pixel.x - xFloor) * (yCeil - pixel.y)
        val x2y2Weight = (pixel.x - xFloor) * (pixel.y - yFloor)
        return x1y1 * x1y1Weight + x1y2 * x1y2Weight + x2y1 * x2y1Weight + x2y2 * x2y2Weight
    }
}

class BicubicInterpolator : PixelInterpolator {
    private val rowVals = FloatArray(4)

    private fun cubic(t: Float): Float {
        val tAbs = abs(t)
        return when {
            tAbs <= 1f -> SolMath.polynomial(tAbs.toDouble(), 1.0, 0.0, -2.5, 1.5).toFloat()
            tAbs <= 2f -> SolMath.polynomial(tAbs.toDouble(), 2.0, -4.0, 2.5, -0.5).toFloat()
            else -> 0f
        }
    }

    override suspend fun interpolate(
        pixel: PixelCoordinate,
        getValue: suspend (x: Int, y: Int) -> Float?
    ): Float? {
        val x = pixel.x
        val y = pixel.y

        val xInt = floor(x).toInt()
        val yInt = floor(y).toInt()

        val fx = x - xInt
        val fy = y - yInt

        for (i in 0 until 4) {
            var value = 0f
            for (j in 0 until 4) {
                val currentX = xInt + j - 1
                val currentY = yInt + i - 1
                val pixelValue = getValue(currentX, currentY)
                    ?: return null

                if (pixelValue.isNaN()) {
                    return null
                }

                value += pixelValue * cubic(fx - (j - 1).toFloat())
            }
            rowVals[i] = value
        }

        var result = 0f
        for (i in 0 until 4) {
            result += rowVals[i] * cubic(fy - (i - 1).toFloat())
        }

        return result
    }
}