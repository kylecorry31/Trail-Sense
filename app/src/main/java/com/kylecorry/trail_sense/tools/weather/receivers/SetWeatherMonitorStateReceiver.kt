package com.kylecorry.trail_sense.tools.weather.receivers

import android.content.Context
import android.os.Bundle
import com.kylecorry.trail_sense.tools.tools.infrastructure.Receiver
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorIsEnabled
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherUpdateScheduler

// TODO: Add support for enable/disable weather monitor
class SetWeatherMonitorStateReceiver : Receiver {
    override fun onReceive(context: Context, data: Bundle) {
        val desiredState = data.getBoolean(PARAM_WEATHER_MONITOR_STATE, false)

        if (desiredState && WeatherMonitorIsEnabled().isSatisfiedBy(context)) {
            WeatherUpdateScheduler.start(context)
        } else if (!desiredState) {
            WeatherUpdateScheduler.stop(context)
        }
    }

    companion object {
        const val PARAM_WEATHER_MONITOR_STATE = "state"
    }
}