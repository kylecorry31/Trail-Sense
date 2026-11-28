package com.kylecorry.trail_sense.tools.weather.domain.forecasting.model

import com.kylecorry.sol.math.classifiers.NeuralNetwork
import kotlin.math.exp

internal class WeatherNetwork {

    private val network = run {
        val sizes = listOf(WeatherNetworkCoefficients.features.size) + WeatherNetworkCoefficients.hiddenLayerSizes
        val hidden = sizes.zipWithNext { inputs, outputs -> NeuralNetwork.Layer.relu(inputs, outputs) }
        val output = NeuralNetwork.Layer.linear(sizes.last(), WeatherOutput.entries.size)
        NeuralNetwork(hidden + output, WeatherNetworkCoefficients.layers)
    }

    /**
     * @param features the raw value of every feature in [WeatherNetworkCoefficients.features], keyed by name
     * @return the probability of each output from 0 to 1. The outputs in a softmax group can't happen together, so
     * their probabilities add up to at most 1.
     */
    fun predict(features: Map<String, Float>): Map<WeatherOutput, Float> {
        val logits = WeatherOutput.entries.zip(
            network.predict(WeatherNetworkCoefficients.features.map { features.getValue(it) })
        ).toMap()
        val grouped = WeatherNetworkCoefficients.softmaxGroups.flatMap { softmax(it, logits).entries }
        val probabilities = logits.mapValues { 1 / (1 + exp(-it.value)) }.toMutableMap()
        grouped.forEach { probabilities[it.key] = it.value }
        return probabilities
    }

    /**
     * A softmax over the group and the case where none of the group happens, which has a logit of 0
     */
    private fun softmax(group: List<WeatherOutput>, logits: Map<WeatherOutput, Float>): Map<WeatherOutput, Float> {
        val largest = maxOf(0f, group.maxOf { logits.getValue(it) })
        val exponentials = group.associateWith { exp(logits.getValue(it) - largest) }
        val total = exp(-largest) + exponentials.values.sum()
        return exponentials.mapValues { it.value / total }
    }
}
