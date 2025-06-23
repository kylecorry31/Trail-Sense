package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.sol.math.SolMath
import kotlin.math.max
import kotlin.math.min

object MarchingSquares {

    fun <T> getIsolineCalculators(
        grid: List<List<Pair<T, Float>>>,
        threshold: Float,
        interpolator: (percent: Float, a: T, b: T) -> T,
    ): List<() -> List<Pair<T, T>>> {
        val squares = mutableListOf<List<Pair<T, Float>>>()
        for (i in 0 until grid.size - 1) {
            for (j in 0 until grid[i].size - 1) {
                val square = listOf(
                    grid[i][j],
                    grid[i][j + 1],
                    grid[i + 1][j + 1],
                    grid[i + 1][j]
                )
                squares.add(square)
            }
        }

        return squares.map { square ->
            {
                marchingSquares(square, threshold, interpolator)
            }
        }
    }

    fun <T> getIsoline(
        grid: List<List<Pair<T, Float>>>,
        threshold: Float,
        interpolator: (percent: Float, a: T, b: T) -> T
    ): List<Pair<T, T>> {
        val calculators = getIsolineCalculators(grid, threshold, interpolator)
        return calculators.flatMap { it() }
    }

    private fun <T> marchingSquares(
        square: List<Pair<T, Float>>,
        threshold: Float,
        interpolator: (Float, T, T) -> T
    ): List<Pair<T, T>> {
        val contourLines = mutableListOf<Pair<T, T>>()


        /**
         *
         *   A--AB--B
         *   |      |
         *  AC     BD
         *   |      |
         *   C--CD--D
         */

        val a = square[0]
        val b = square[1]
        val c = square[3]
        val d = square[2]

        val ab = getInterpolatedPoint(threshold, a, b, interpolator)
        val ac = getInterpolatedPoint(threshold, a, c, interpolator)
        val bd = getInterpolatedPoint(threshold, b, d, interpolator)
        val cd = getInterpolatedPoint(threshold, c, d, interpolator)

        // If there are exactly 2 intersections, then there is 1 line
        val intersections = listOfNotNull(ab, ac, bd, cd)
        if (intersections.size == 2) {
            contourLines.add(intersections[0] to intersections[1])
        } else if (intersections.size == 4 && a.second >= threshold) {
            contourLines.add(intersections[0] to intersections[2])
            contourLines.add(intersections[1] to intersections[3])
        } else if (intersections.size == 4 && a.second < threshold) {
            contourLines.add(intersections[0] to intersections[1])
            contourLines.add(intersections[2] to intersections[3])
        }
        return contourLines
    }

    private fun <T> getInterpolatedPoint(
        value: Float,
        a: Pair<T, Float>,
        b: Pair<T, Float>,
        interpolator: (Float, T, T) -> T
    ): T? {
        val aAbove = a.second >= value
        val bAbove = b.second >= value

        if (aAbove == bAbove) {
            return null
        }

        var pct = SolMath.norm(value, min(a.second, b.second), max(a.second, b.second))
        if (a.second > b.second) {
            pct = 1 - pct
        }
        return interpolator(pct, a.first, b.first)
    }

}