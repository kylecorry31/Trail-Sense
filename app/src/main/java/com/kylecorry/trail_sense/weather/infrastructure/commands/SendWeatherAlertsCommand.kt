package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.shared.commands.generic.ComposedCommand
import com.kylecorry.trail_sense.weather.domain.CurrentWeather

class SendWeatherAlertsCommand(private val context: Context) : Command<CurrentWeather> {

    override fun execute(weather: CurrentWeather) {
        val command = ComposedCommand(
            DailyWeatherAlertCommand.create(context),
            StormAlertCommand.create(context),
            CurrentWeatherAlertCommand.create(context)
        )
        command.execute(weather)
    }

}