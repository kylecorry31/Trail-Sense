package com.kylecorry.trail_sense.tools.weather.actions

import android.content.Context
import android.os.Bundle
import com.kylecorry.trail_sense.tools.tools.infrastructure.Action
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration

class ResumeWeatherMonitorAction : Action {
    override suspend fun onReceive(context: Context, data: Bundle) {
        val service = Tools.getService(context, WeatherToolRegistration.SERVICE_WEATHER_MONITOR)
        service?.restart()
    }
}