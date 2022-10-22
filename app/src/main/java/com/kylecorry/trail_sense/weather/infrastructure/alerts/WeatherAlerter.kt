package com.kylecorry.trail_sense.weather.infrastructure.alerts

import android.content.Context
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.weather.infrastructure.CurrentWeather
import com.kylecorry.trail_sense.weather.infrastructure.commands.CurrentWeatherAlertCommand
import com.kylecorry.trail_sense.weather.infrastructure.commands.DailyWeatherAlertCommand
import com.kylecorry.trail_sense.weather.infrastructure.commands.StormAlertCommand

class WeatherAlerter(private val context: Context) : IValueAlerter<CurrentWeather> {

    override fun alert(value: CurrentWeather) {
        val commands = listOfNotNull(
            DailyWeatherAlertCommand.create(context, value.prediction),
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