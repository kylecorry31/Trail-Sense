package com.kylecorry.trail_sense.tools.ai_assistant.domain

import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather

class WeatherAiContextProvider(
    private val weatherProvider: suspend () -> CurrentWeather
) : AiContextProvider {

    override val toolId: String = "weather"

    override suspend fun getAiContext(): AiContext {
        val weather = weatherProvider()
        val obs = weather.observation
        val tendency = weather.pressureTendency

        val prediction = weather.prediction

        val sensorData = mutableMapOf<String, Any>()
        if (obs != null) {
            sensorData["pressure_hpa"] = obs.pressure.hpa().pressure
            sensorData["temperature_c"] = obs.temperature.celsius().temperature
            obs.humidity?.let { sensorData["humidity_percent"] = it }
        }
        sensorData["pressure_characteristic"] = tendency.characteristic.name
        sensorData["pressure_change_hpa"] = tendency.amount

        val hourlyConditions = prediction.primaryHourly?.name ?: "Unknown"
        val dailyConditions = prediction.primaryDaily?.name ?: "Unknown"
        sensorData["forecast_hourly"] = hourlyConditions
        sensorData["forecast_daily"] = dailyConditions

        val summary = buildString {
            append("Weather Tool Data:\n")
            if (obs != null) {
                append("- Pressure: ${obs.pressure.hpa().pressure} hPa\n")
                append("- Temperature: ${obs.temperature.celsius().temperature}°C\n")
                obs.humidity?.let { append("- Humidity: ${it}%\n") }
            }
            append("- Pressure trend: ${tendency.characteristic.name} (${tendency.amount} hPa)\n")
            append("- Hourly forecast: $hourlyConditions\n")
            append("- Daily forecast: $dailyConditions\n")
        }

        return AiContext(
            toolId = toolId,
            toolName = "Weather",
            sensorData = sensorData,
            image = null,
            summary = summary
        )
    }

    override fun getSuggestedQuestions(): List<String> {
        return listOf(
            "What does this pressure trend mean?",
            "Is it safe to stay outdoors?",
            "What weather should I expect in the next few hours?"
        )
    }
}
