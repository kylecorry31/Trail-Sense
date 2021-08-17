package com.kylecorry.trail_sense.tiles

import android.os.Build
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.services.AndromedaTileService
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler

@RequiresApi(Build.VERSION_CODES.N)
class WeatherMonitorTile : AndromedaTileService() {

    private val prefs by lazy { UserPreferences(this) }
    private val formatService by lazy { FormatServiceV2(this) }

    override fun isOn(): Boolean {
        return prefs.weather.shouldMonitorWeather && !isDisabled()
    }

    override fun isDisabled(): Boolean {
        return !Sensors.hasBarometer(this) || (prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesWeather)
    }

    override fun onInterval() {
        setSubtitle(formatService.formatDuration(prefs.weather.weatherUpdateFrequency))
    }

    override fun start() {
        prefs.weather.shouldMonitorWeather = true
        WeatherUpdateScheduler.start(this)
    }

    override fun stop() {
        prefs.weather.shouldMonitorWeather = false
        WeatherUpdateScheduler.stop(this)
    }
}