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

    fun assertWithin(
        errors: List<Float>,
        limit: Float,
        minFraction: Float,
        modelName: String = ""
    ) {
        val fraction = errors.count { it.absoluteValue <= limit } / errors.size.toFloat()
        println("$modelName within $limit: $fraction")
        assert(fraction >= minFraction) {
            "$modelName within $limit below threshold: $fraction < $minFraction"
        }
    }

    // The median of the signed errors, which shows a systematic offset that absolute errors hide
    fun assertBias(
        errors: List<Float>,
        threshold: Float,
        modelName: String = ""
    ) {
        val bias = Statistics.quantile(errors, 0.5f)
        println("$modelName bias: $bias")
        assert(bias.absoluteValue <= threshold) {
            "$modelName bias exceeded threshold: $bias beyond +/-$threshold"
        }
    }
}
