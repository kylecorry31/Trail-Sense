package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorChecker

class WeatherPreferences(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val sensorChecker = SensorChecker(context)

    val shouldMonitorWeather: Boolean
        get() = sensorChecker.hasBarometer()

    val useSeaLevelPressure: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_use_sea_level_pressure), true)

    val sendStormAlerts: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_send_storm_alert), true)

    val stormAlertThreshold: Float
        get() = -6.0f

}