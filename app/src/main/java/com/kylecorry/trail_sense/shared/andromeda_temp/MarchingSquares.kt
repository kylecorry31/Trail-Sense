package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.sol.math.SolMath
import kotlin.math.max
import kotlin.math.min

object MarchingSquares {

    fun <T> getIsolineCalculators(
        grid: List<List<Pair<T, Float>>>,
        threshold: Float,
        interpolator: (percent: Float, a: T, b: T) -> T,
        angleCalculator: (T, T) -> Float = { a, b -> 0f }
    ): List<() -> List<IsolineSegment<T>>> {
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
                marchingSquares(square, threshold, interpolator, angleCalculator)
            }
        }
    }

    fun <T> getIsoline(
        grid: List<List<Pair<T, Float>>>,
        threshold: Float,
        interpolator: (percent: Float, a: T, b: T) -> T,
        angleCalculator: (T, T) -> Float = { a, b -> 0f }
    ): List<IsolineSegment<T>> {
        val calculators = getIsolineCalculators(grid, threshold, interpolator, angleCalculator)
        return calculators.flatMap { it() }
    }

    private fun <T> marchingSquares(
        square: List<Pair<T, Float>>,
        threshold: Float,
        interpolator: (Float, T, T) -> T,
        angleCalculator: (T, T) -> Float = { a, b -> 0f }
    ): List<IsolineSegment<T>> {
        val contourLines = mutableListOf<IsolineSegment<T>>()


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

        val cornersAsNumber = listOf(
            if (a.second >= threshold) 1 else 0,
            if (b.second >= threshold) 2 else 0,
            if (c.second >= threshold) 4 else 0,
            if (d.second >= threshold) 8 else 0
        ).sum()

        val perpendicularDirection = when (cornersAsNumber) {
            0b1100, 0b1101, 0b0100, 0b0101, 0b0111, 0b0001 -> -90f
            0b1000, 0b1110, 0b1011, 0b1010, 0b0011 -> 90f
            else -> 0f
        }

        // If there are exactly 2 intersections, then there is 1 line
        val intersections = listOfNotNull(ab, ac, bd, cd)
        if (intersections.size == 2) {
            val angle = angleCalculator(intersections[0], intersections[1])
            val slopeAngle = angle + perpendicularDirection

            contourLines.add(IsolineSegment(intersections[0], intersections[1], slopeAngle))
        } else if (intersections.size == 4 && a.second >= threshold) {
            val angle1 = angleCalculator(intersections[0], intersections[1])
            val angle2 = angleCalculator(intersections[2], intersections[3])
            contourLines.add(IsolineSegment(intersections[0], intersections[2], angle1 + 90f))
            contourLines.add(IsolineSegment(intersections[1], intersections[3], angle2 - 90f))
        } else if (intersections.size == 4 && a.second < threshold) {
            val angle1 = angleCalculator(intersections[0], intersections[1])
            val angle2 = angleCalculator(intersections[2], intersections[3])
            contourLines.add(IsolineSegment(intersections[0], intersections[1], angle1 - 90f))
            contourLines.add(IsolineSegment(intersections[2], intersections[3], angle2 + 90f))
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