package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration

class WeatherMonitorRequiresForeground: Specification<Context>() {
    override fun isSatisfiedBy(value: Context): Boolean {
        val prefs = UserPreferences(value)
        return prefs.weather.weatherUpdateFrequency >= Duration.ofMinutes(15)
    }
}