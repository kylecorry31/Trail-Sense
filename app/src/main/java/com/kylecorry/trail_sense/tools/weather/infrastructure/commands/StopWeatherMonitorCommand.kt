package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem

class StopWeatherMonitorCommand(private val context: Context): Command {
    override fun execute() {
        WeatherSubsystem.getInstance(context).disableMonitor()
    }
}