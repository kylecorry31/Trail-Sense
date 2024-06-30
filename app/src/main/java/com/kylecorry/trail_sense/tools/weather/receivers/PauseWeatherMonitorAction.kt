package com.kylecorry.trail_sense.tools.weather.receivers

import android.content.Context
import android.os.Bundle
import com.kylecorry.trail_sense.tools.tools.infrastructure.Action
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorIsEnabled
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherUpdateScheduler

class PauseWeatherMonitorAction : Action {
    override fun onReceive(context: Context, data: Bundle) {
        WeatherUpdateScheduler.stop(context)
    }
}