package com.kylecorry.trail_sense.main.automations

import com.kylecorry.trail_sense.shared.automations.Automation
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration

object WidgetAutomations {
    fun refresh(): List<Automation> {
        return listOf(
            Automation(
                WeatherToolRegistration.BROADCAST_WEATHER_PREDICTION_CHANGED,
                listOf(WeatherToolRegistration.ACTION_REFRESH_WEATHER_WIDGET)
            )
        )
    }
}