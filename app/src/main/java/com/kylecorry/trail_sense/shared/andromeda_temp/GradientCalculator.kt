package com.kylecorry.trail_sense.shared.andromeda_temp

import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.geometry.Gradients
import kotlin.math.abs
import kotlin.math.hypot

class GradientCalculator {

    private val blurRadius = 2

    fun calculate(pixels: IntArray, width: Int, height: Int): Gradients {
        val blurred = blur(pixels, width, height)

        val gx = Matrix.create(height, width)
        val gy = Matrix.create(height, width)
        val magnitude = Matrix.create(height, width)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val topLeft = blurred[(y - 1) * width + x - 1].toFloat()
                val top = blurred[(y - 1) * width + x].toFloat()
                val topRight = blurred[(y - 1) * width + x + 1].toFloat()
                val left = blurred[y * width + x - 1].toFloat()
                val right = blurred[y * width + x + 1].toFloat()
                val bottomLeft = blurred[(y + 1) * width + x - 1].toFloat()
                val bottom = blurred[(y + 1) * width + x].toFloat()
                val bottomRight = blurred[(y + 1) * width + x + 1].toFloat()

                val dx = -topLeft + topRight - 2 * left + 2 * right - bottomLeft + bottomRight
                val dy = -topLeft - 2 * top - topRight + bottomLeft + 2 * bottom + bottomRight
                gx[y, x] = dx
                gy[y, x] = dy
                magnitude[y, x] = hypot(dx, dy)
            }
        }

        return Gradients(gx, gy, magnitude)
    }


    private fun blur(pixels: IntArray, width: Int, height: Int): IntArray {
        val blurred = IntArray(pixels.size)
        // Convert to grayscale
        for (i in pixels.indices) {
            val color = pixels[i]
            blurred[i] = (color.red * 30 + color.green * 59 + color.blue * 11) / 100
        }

        val horizontal = IntArray(blurred.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var total = 0
                var weight = 0
                for (dx in -blurRadius..blurRadius) {
                    val nx = x + dx
                    if (nx in 0 until width) {
                        val w = getWeight(abs(dx))
                        total += blurred[y * width + nx] * w
                        weight += w
                    }
                }
                horizontal[y * width + x] = total / weight
            }
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                var total = 0
                var weight = 0
                for (dy in -blurRadius..blurRadius) {
                    val ny = y + dy
                    if (ny in 0 until height) {
                        val w = getWeight(abs(dy))
                        total += horizontal[ny * width + x] * w
                        weight += w
                    }
                }
                blurred[y * width + x] = total / weight
            }
        }

        return blurred
    }

    private fun getWeight(distance: Int): Int {
        return when (distance) {
            0 -> 6
            1 -> 4
            else -> 1
        }
    }

}
