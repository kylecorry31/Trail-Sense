package com.kylecorry.trail_sense.tools.ai_assistant.domain

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherPrediction
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class WeatherAiContextProviderTest {

    @Test
    fun `getAiContext includes pressure and tendency in sensorData`() = runBlocking {
        val weather = CurrentWeather(
            prediction = WeatherPrediction(
                hourly = listOf(WeatherCondition.Storm),
                daily = listOf(WeatherCondition.Storm),
                front = null,
                hourlyArrival = null,
                temperature = null,
                alerts = emptyList()
            ),
            pressureTendency = PressureTendency(
                PressureCharacteristic.FallingFast,
                -4.1f
            ),
            observation = WeatherObservation(
                id = 1L,
                time = Instant.now(),
                pressure = Pressure.hpa(1008.2f),
                temperature = Temperature.celsius(20f),
                humidity = 65f
            ),
            clouds = null
        )

        val provider = WeatherAiContextProvider { weather }
        val context = provider.getAiContext()

        assertEquals("weather", context.toolId)
        assertEquals(1008.2f, context.sensorData["pressure_hpa"])
        assertEquals("FallingFast", context.sensorData["pressure_characteristic"])
        assertEquals(-4.1f, context.sensorData["pressure_change_hpa"])
        assertNull(context.image)
        assertTrue(context.summary.contains("1008.2"))
    }

    @Test
    fun `getSuggestedQuestions returns non-empty list`() {
        val provider = WeatherAiContextProvider { error("unused") }
        val questions = provider.getSuggestedQuestions()
        assertTrue(questions.isNotEmpty())
    }
}
