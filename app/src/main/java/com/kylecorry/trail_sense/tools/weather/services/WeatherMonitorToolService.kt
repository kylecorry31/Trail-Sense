package com.kylecorry.trail_sense.tools.weather.services

import android.content.Context
import android.util.Log
import androidx.core.os.bundleOf
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.receivers.ServiceRestartAlerter
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.tryStartForegroundOrNotify
import com.kylecorry.trail_sense.shared.permissions.canStartLocationForgroundService
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorService
import java.time.Duration

class WeatherMonitorToolService(private val context: Context) : ToolService {

    private val prefs = UserPreferences(context)
    private val sharedPreferences = PreferencesSubsystem.getInstance(context).preferences
    private val stateChangePrefKeys = listOf(
        R.string.pref_monitor_weather,
        R.string.pref_low_power_mode,
        R.string.pref_low_power_mode_weather
    ).map { context.getString(it) }

    private val frequencyChangePrefKeys = listOf(
        R.string.pref_weather_update_frequency
    ).map { context.getString(it) }

    override val id: String = WeatherToolRegistration.SERVICE_WEATHER_MONITOR

    override val name: String = context.getString(R.string.weather_monitor)

    init {
        sharedPreferences.onChange.subscribe(this::onPreferencesChanged)
    }

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
        WeatherMonitorService.stop(context)
        Notify.cancel(context, WeatherMonitorService.WEATHER_NOTIFICATION_ID)
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

        if (!hasPermissions(context)) {
            ServiceRestartAlerter(context).alert()
            Log.d("WeatherUpdateScheduler", "Cannot start weather monitoring")
            return
        }

        tryStartForegroundOrNotify(context) {
            WeatherMonitorService.start(context)
        }
    }

    private fun onPreferencesChanged(preference: String): Boolean {
        if (preference in stateChangePrefKeys) {
            Tools.broadcast(WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_STATE_CHANGED)
        }

        if (preference in frequencyChangePrefKeys) {
            Tools.broadcast(
                WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_FREQUENCY_CHANGED,
                bundleOf(
                    WeatherToolRegistration.BROADCAST_PARAM_WEATHER_MONITOR_FREQUENCY to prefs.weather.weatherUpdateFrequency.toMillis()
                )
            )
        }

        return true
    }

    private fun hasPermissions(context: Context): Boolean {
        // Either it didn't need location or it has foreground location permission (runtime check)
        return !Permissions.canGetLocation(context) || Permissions.canStartLocationForgroundService(
            context
        )
    }

    protected fun finalize() {
        sharedPreferences.onChange.unsubscribe(this::onPreferencesChanged)
    }
}