package com.kylecorry.trail_sense.tools.weather.domain.forecasting.learned

import com.kylecorry.sol.math.classifiers.BinaryLogisticClassifier

internal class LearnedCondition(
    private val features: List<String>,
    private val classifier: BinaryLogisticClassifier
) {

    fun margin(values: Map<String, Float>): Float {
        return classifier.margin(features.map { values.getValue(it) })
    }

    fun predict(values: Map<String, Float>): Boolean {
        return classifier.predict(features.map { values.getValue(it) })
    }
}
