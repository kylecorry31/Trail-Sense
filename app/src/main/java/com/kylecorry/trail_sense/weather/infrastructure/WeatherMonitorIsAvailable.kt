package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.shared.UserPreferences

class WeatherMonitorIsAvailable : Specification<Context>() {
    override fun isSatisfiedBy(value: Context): Boolean {
        val prefs = UserPreferences(value)
        return prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesWeather && Sensors.hasBarometer(
            value
        )
    }
}