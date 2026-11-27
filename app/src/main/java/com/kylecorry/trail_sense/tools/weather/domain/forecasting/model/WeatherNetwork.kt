package com.kylecorry.trail_sense.tools.weather.domain.forecasting.model

import com.kylecorry.sol.math.classifiers.NeuralNetwork

internal class WeatherNetwork {

    private val network = NeuralNetwork(
        listOf(
            NeuralNetwork.Layer.relu(WeatherNetworkCoefficients.features.size, WeatherNetworkCoefficients.HIDDEN_UNITS),
            NeuralNetwork.Layer.sigmoid(WeatherNetworkCoefficients.HIDDEN_UNITS, WeatherOutput.entries.size)
        ),
        listOf(WeatherNetworkCoefficients.hidden, WeatherNetworkCoefficients.output)
    )

    /**
     * @param features the raw value of every feature in [WeatherNetworkCoefficients.features], keyed by name
     * @return the probability of each output from 0 to 1
     */
    fun predict(features: Map<String, Float>): Map<WeatherOutput, Float> {
        val values = network.predict(WeatherNetworkCoefficients.features.map { features.getValue(it) })
        return WeatherOutput.entries.zip(values).toMap()
    }
}
