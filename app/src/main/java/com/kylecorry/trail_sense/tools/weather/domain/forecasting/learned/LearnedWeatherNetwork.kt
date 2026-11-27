package com.kylecorry.trail_sense.tools.weather.domain.forecasting.learned

import com.kylecorry.sol.math.classifiers.NeuralNetwork

internal class LearnedWeatherNetwork {

    private val network = NeuralNetwork(
        listOf(
            NeuralNetwork.Layer.relu(LearnedWeatherCoefficients.features.size, LearnedWeatherCoefficients.HIDDEN_UNITS),
            NeuralNetwork.Layer.sigmoid(LearnedWeatherCoefficients.HIDDEN_UNITS, LearnedOutput.entries.size)
        ),
        listOf(LearnedWeatherCoefficients.hidden, LearnedWeatherCoefficients.output)
    )

    /**
     * @param features the raw value of every feature in [LearnedWeatherCoefficients.features], keyed by name
     * @return the probability of each output from 0 to 1
     */
    fun predict(features: Map<String, Float>): Map<LearnedOutput, Float> {
        val values = network.predict(LearnedWeatherCoefficients.features.map { features.getValue(it) })
        return LearnedOutput.entries.zip(values).toMap()
    }
}
