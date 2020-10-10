package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import java.time.Duration
import java.time.Instant

class WeatherPreferences(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val sensorChecker = SensorChecker(context)
    private val cache = Cache(context)

    val hasBarometer: Boolean
        get() = sensorChecker.hasBarometer()

    val hasThermometer: Boolean
        get() = sensorChecker.hasThermometer()

    var temperatureAdjustment: Int
        get() = prefs.getInt(context.getString(R.string.pref_temperature_adjustment_c), 0)
        set(value) = prefs.edit {
            putInt(
                context.getString(R.string.pref_temperature_adjustment_c),
                value
            )
        }

    val shouldMonitorWeather: Boolean
        get() = sensorChecker.hasBarometer() && prefs.getBoolean(
            context.getString(R.string.pref_monitor_weather),
            true
        )

    val weatherUpdateFrequency: Duration
        get() {
            val raw =
                prefs.getString(context.getString(R.string.pref_weather_update_frequency), null)
                    ?: "15"
            return Duration.ofMinutes(raw.toLongOrNull() ?: 15)
        }

    val shouldShowWeatherNotification: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_show_weather_notification), true)

    val shouldShowPressureInNotification: Boolean
        get() = prefs.getBoolean(
            context.getString(R.string.pref_show_pressure_in_notification),
            false
        )

    val useSeaLevelPressure: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_use_sea_level_pressure), true)

    val seaLevelFactorInTemp: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_adjust_for_temperature), false)

    val seaLevelFactorInRapidChanges: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_sea_level_use_rapid), true)

    val pressureHistory: Duration
        get() {
            val raw =
                prefs.getString(context.getString(R.string.pref_pressure_history), "48") ?: "48"
            return Duration.ofHours(raw.toLong())
        }

    val requireDwell: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_sea_level_require_dwell), false)

    val maxNonTravellingAltitudeChange: Float
        get() = cache.getInt(context.getString(R.string.pref_barometer_altitude_change))?.toFloat() ?: 60f

    val maxNonTravellingPressureChange: Float
        get() = 20 * (cache.getInt(context.getString(R.string.pref_sea_level_pressure_change_thresh))?.toFloat() ?: 50f) / 200f

    val foregroundService: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_weather_foreground_service))
            ?: true

    val forceWeatherUpdates: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_force_weather_updates)) ?: false

    val sendStormAlerts: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_send_storm_alert), true)

    val dailyForecastChangeThreshold: Float
        get() {
            return when (prefs.getString(
                context.getString(R.string.pref_forecast_sensitivity),
                "medium"
            ) ?: "medium") {
                "low" -> 0.75f
                "medium" -> 0.5f
                else -> 0.3f
            }
        }

    val hourlyForecastChangeThreshold: Float
        get() {
            return when (prefs.getString(
                context.getString(R.string.pref_forecast_sensitivity),
                "medium"
            ) ?: "medium") {
                "low" -> 2.5f
                "medium" -> 1.5f
                else -> 0.5f
            }
        }

    val stormAlertThreshold: Float
        get() {
            if (!sendStormAlerts) {
                return -4.5f
            }

            return when (prefs.getString(
                context.getString(R.string.pref_storm_alert_sensitivity),
                "medium"
            ) ?: "medium") {
                "low" -> -6f
                "medium" -> -4.5f
                else -> -3f
            }
        }

    var pressureSetpoint: PressureAltitudeReading?
        get() {
            val pressure = cache.getFloat("cache_pressure_setpoint") ?: return null
            val altitude = cache.getFloat("cache_pressure_setpoint_altitude") ?: 0f
            val temperature = cache.getFloat("cache_pressure_setpoint_temperature") ?: 16f
            val time = Instant.ofEpochMilli(
                cache.getLong("cache_pressure_setpoint_time") ?: Instant.MIN.toEpochMilli()
            )

            return PressureAltitudeReading(time, pressure, altitude, temperature)
        }
        set(value) {
            if (value == null) {
                cache.remove("cache_pressure_setpoint")
                return
            }

            cache.putFloat("cache_pressure_setpoint", value.pressure)
            cache.putFloat("cache_pressure_setpoint_altitude", value.altitude)
            cache.putFloat("cache_pressure_setpoint_temperature", value.temperature)
            cache.putLong("cache_pressure_setpoint_time", value.time.toEpochMilli())
        }

}