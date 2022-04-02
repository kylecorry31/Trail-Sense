package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler

class StopWeatherMonitorCommand(private val context: Context): Command {
    override fun execute() {
        val prefs = UserPreferences(context)
        prefs.weather.shouldMonitorWeather = false
        WeatherUpdateScheduler.stop(context)
    }
}