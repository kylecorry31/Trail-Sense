package com.kylecorry.trail_sense.shared.data

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.optimization.Extremum
import com.kylecorry.sol.math.optimization.IExtremaFinder
import com.kylecorry.sol.math.optimization.SimpleExtremaFinder
import kotlin.math.abs

class GoldenRatioExtremaFinder(
    private val initialStep: Double,
    private val tolerance: Double,
    finderProducer: (step: Double) -> IExtremaFinder = { SimpleExtremaFinder(it) }
) : IExtremaFinder {

    private val initialFinder = finderProducer(initialStep)

    private val goldenRatio = 1.61803398875

    override fun find(range: Range<Double>, fn: (x: Double) -> Double): List<Extremum> {
        // Get the initial extremas
        val extremas = initialFinder.find(range, fn).toMutableList()

        // For each extrema, use the golden ratio search to fine tune it
        for (i in extremas.indices) {
            val extrema = extremas[i]
            val extremaRange = createRange(extrema, initialStep)
            val fineTuned = goldenRatioSearch(extremaRange, fn, extrema.isHigh)

            extremas[i] = fineTuned
        }

        return extremas
    }


    private fun createRange(extremum: Extremum, stepSize: Double): Range<Double> {
        val start = extremum.point.x - stepSize
        val end = extremum.point.x + stepSize
        return Range(start, end)
    }

    // TODO: Extract this
    private fun goldenRatioSearch(
        range: Range<Double>,
        fn: (x: Double) -> Double,
        isHigh: Boolean
    ): Extremum {
        var a = range.start
        var b = range.end
        var c = b - (b - a) / goldenRatio
        var d = a + (b - a) / goldenRatio

        val maxIterations = ((range.end - range.start) / tolerance).toInt()
        var iteration = 0
        while (abs(c - d) > tolerance && iteration < maxIterations) {
            iteration++
            if (fn(c) < fn(d)) {
                if (isHigh) {
                    a = c
                } else {
                    b = d
                }
            } else {
                if (isHigh) {
                    b = d
                } else {
                    a = c
                }
            }
            c = b - (b - a) / goldenRatio
            d = a + (b - a) / goldenRatio
        }
        return Extremum(Vector2(c.toFloat(), fn(c).toFloat()), isHigh)
    }
}