package com.kylecorry.trail_sense.tools.weather.receivers

import android.content.Context
import android.os.Bundle
import com.kylecorry.trail_sense.tools.tools.infrastructure.Action
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorIsEnabled
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherUpdateScheduler

class ResumeWeatherMonitorAction : Action {
    override suspend fun onReceive(context: Context, data: Bundle) {
        if (WeatherMonitorIsEnabled().isSatisfiedBy(context)) {
            WeatherUpdateScheduler.start(context)
        }
    }
}