package com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.temp

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.statistics.Statistics

class TimeSeriesEnsemble(
    private val predictors: List<ITimeSeriesPredictor>,
    private val confidenceIntervalSize: Float = 0.95f
) {
    fun predictNext(
        samples: List<Vector2>,
        n: Int,
        step: Float? = null
    ): List<ConfidenceInterval<Vector2>> {
        val predictions = predictors.map { it.predictNext(samples, n, step) }
        val result = mutableListOf<ConfidenceInterval<Vector2>>()
        for (i in 0 until n) {
            val x = predictions.firstOrNull()?.getOrNull(i)?.x ?: continue
            val values = predictions.mapNotNull { it.getOrNull(i)?.y }
            if (values.isEmpty()) {
                continue
            }
            val median = Statistics.median(values)
            val lower = Statistics.quantile(values, 1f - confidenceIntervalSize)
            val upper = Statistics.quantile(values, confidenceIntervalSize)
            result.add(ConfidenceInterval(Vector2(x, median), Vector2(x, upper), Vector2(x, lower)))
        }
        return result
    }
}