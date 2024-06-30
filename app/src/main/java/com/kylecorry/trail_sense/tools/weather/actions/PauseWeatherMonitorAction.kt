package com.kylecorry.trail_sense.tools.weather.actions

import android.content.Context
import android.os.Bundle
import com.kylecorry.trail_sense.tools.tools.infrastructure.Action
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherUpdateScheduler

class PauseWeatherMonitorAction : Action {
    override suspend fun onReceive(context: Context, data: Bundle) {
        WeatherUpdateScheduler.stop(context)
    }
}