package com.kylecorry.trail_sense.tools.weather.domain.forecasting.model

import com.kylecorry.sol.math.classifiers.NeuralNetwork

internal class WeatherNetwork {

    private val network = run {
        val sizes = listOf(WeatherNetworkCoefficients.features.size) + WeatherNetworkCoefficients.hiddenLayerSizes
        val hidden = sizes.zipWithNext { inputs, outputs -> NeuralNetwork.Layer.relu(inputs, outputs) }
        val output = NeuralNetwork.Layer.sigmoid(sizes.last(), WeatherOutput.entries.size)
        NeuralNetwork(hidden + output, WeatherNetworkCoefficients.layers)
    }

    /**
     * @param features the raw value of every feature in [WeatherNetworkCoefficients.features], keyed by name
     * @return the probability of each output from 0 to 1
     */
    fun predict(features: Map<String, Float>): Map<WeatherOutput, Float> {
        val values = network.predict(WeatherNetworkCoefficients.features.map { features.getValue(it) })
        return WeatherOutput.entries.zip(values).toMap()
    }
}
