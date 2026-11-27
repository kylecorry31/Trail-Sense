package com.kylecorry.trail_sense.tools.weather.domain.forecasting.learned

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LearnedWeatherNetworkTest {

    @Test
    fun theNetworkMatchesTheProbabilitiesFromTraining() {
        val lines = javaClass.getResourceAsStream("/weather_benchmark/learned_network_golden.csv")!!
            .bufferedReader().use { it.readLines() }.filter { it.isNotBlank() }
        val header = lines.first().split(",")
        val network = LearnedWeatherNetwork()

        lines.drop(1).forEach { line ->
            val values = header.zip(line.split(",").map { it.toFloat() }).toMap()
            val probabilities = network.predict(values)
            LearnedOutput.entries.forEach { output ->
                assertEquals(values.getValue(output.columnName()), probabilities.getValue(output), 1e-4f, output.name)
            }
        }
    }

    private fun LearnedOutput.columnName(): String {
        return name.replace(Regex("(?<=[a-z])(?=[A-Z])"), "_").lowercase()
    }
}
