package com.kylecorry.trail_sense.test_utils

import com.kylecorry.sol.math.statistics.Statistics
import kotlin.math.absoluteValue

object TestStatistics {

    fun assertQuantile(
        errors: List<Float>,
        threshold: Float,
        quantile: Float,
        modelName: String = ""
    ) {
        val absoluteErrors = errors.map { it.absoluteValue }
        val quantileValue = Statistics.quantile(absoluteErrors, quantile)
        println("$modelName quantile $quantile: $quantileValue")
        assert(quantileValue <= threshold) {
            "$modelName quantile $quantile exceeded threshold: $quantileValue > $threshold"
        }
    }
}