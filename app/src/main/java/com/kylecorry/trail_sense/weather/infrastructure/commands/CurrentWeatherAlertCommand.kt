package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.weather.infrastructure.CurrentWeather
import com.kylecorry.trail_sense.weather.infrastructure.IWeatherPreferences
import com.kylecorry.trail_sense.weather.infrastructure.alerts.CurrentWeatherAlerter

internal class CurrentWeatherAlertCommand(
    private val prefs: IWeatherPreferences,
    private val alerter: IValueAlerter<CurrentWeather>
) : Command<CurrentWeather> {
    override fun execute(weather: CurrentWeather) {
        if (prefs.shouldShowWeatherNotification && prefs.shouldMonitorWeather) {
            alerter.alert(weather)
        }
    }

    companion object {
        fun create(context: Context): CurrentWeatherAlertCommand {
            val prefs = UserPreferences(context)
            return CurrentWeatherAlertCommand(
                prefs.weather,
                CurrentWeatherAlerter(
                    context,
                    FormatService.getInstance(context),
                    prefs.pressureUnits,
                    prefs.temperatureUnits,
                    prefs.weather
                )
            )
        }
    }

}