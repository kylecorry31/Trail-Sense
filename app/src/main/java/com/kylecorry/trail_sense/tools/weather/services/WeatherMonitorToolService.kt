package com.kylecorry.trail_sense.tools.weather.services

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService2
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorService
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherUpdateScheduler
import java.time.Duration

class WeatherMonitorToolService(private val context: Context) : ToolService2 {

    private val prefs = UserPreferences(context)

    override val id: String = WeatherToolRegistration.SERVICE_WEATHER_MONITOR

    override val name: String = context.getString(R.string.weather_monitor)

    override fun getFrequency(): Duration {
        return prefs.weather.weatherUpdateFrequency
    }

    override fun isRunning(): Boolean {
        return isEnabled() && !isBlocked()
    }

    override fun isEnabled(): Boolean {
        return prefs.weather.shouldMonitorWeather
    }

    override fun isBlocked(): Boolean {
        return prefs.isLowPowerModeOn && prefs.lowPowerModeDisablesWeather
    }

    override suspend fun enable() {
        prefs.weather.shouldMonitorWeather = true
        Tools.broadcast(WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_ENABLED)
        start()
    }

    override suspend fun disable() {
        prefs.weather.shouldMonitorWeather = false
        Tools.broadcast(WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_DISABLED)
        stop()
    }

    override suspend fun restart() {
        if (isEnabled() && !isBlocked()) {
            start()
        } else {
            stop()
        }
    }

    override suspend fun stop() {
        WeatherUpdateScheduler.stop(context)
        // TODO: Broadcast
    }

    private fun start() {
        if (!isEnabled() || isBlocked()) {
            // Can't start
            return
        }

        if (WeatherMonitorService.isRunning) {
            // Already running
            return
        }

        WeatherUpdateScheduler.start(context)
        // TODO: Broadcast
    }
}