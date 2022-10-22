package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.weather.infrastructure.CurrentWeather

class WeatherAlerter(private val context: Context) : IValueAlerter<CurrentWeather> {

    override fun alert(value: CurrentWeather) {
        val commands = listOfNotNull(
            DailyWeatherAlertCommand(context, value.prediction),
            StormAlertCommand(context, value.prediction),
            value.observation?.let {
                CurrentWeatherAlertCommand(
                    context,
                    value.prediction,
                    value.pressureTendency,
                    it.pressureReading()
                )
            }
        )

        commands.forEach { it.execute() }
    }

}