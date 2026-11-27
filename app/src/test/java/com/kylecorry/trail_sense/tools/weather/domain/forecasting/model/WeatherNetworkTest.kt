package com.kylecorry.trail_sense.tools.weather.domain.forecasting.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeatherNetworkTest {

    @Test
    fun theNetworkMatchesTheProbabilitiesFromTraining() {
        val lines = javaClass.getResourceAsStream("/weather_benchmark/weather_network_golden.csv")!!
            .bufferedReader().use { it.readLines() }.filter { it.isNotBlank() }
        val header = lines.first().split(",")
        val network = WeatherNetwork()

        lines.drop(1).forEach { line ->
            val values = header.zip(line.split(",").map { it.toFloat() }).toMap()
            val probabilities = network.predict(values)
            WeatherOutput.entries.forEach { output ->
                assertEquals(values.getValue(output.columnName()), probabilities.getValue(output), 1e-4f, output.name)
            }
        }
    }

    private fun WeatherOutput.columnName(): String {
        return name.replace(Regex("(?<=[a-z0-9])(?=[A-Z])|(?<=[a-z])(?=[0-9])"), "_").lowercase()
    }
}
