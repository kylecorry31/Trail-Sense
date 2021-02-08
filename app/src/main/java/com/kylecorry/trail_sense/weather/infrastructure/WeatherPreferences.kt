package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.math.MathExtensions.toFloatCompat2
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import java.time.Duration
import java.time.Instant

class WeatherPreferences(private val context: Context) {

    private val sensorChecker = SensorChecker(context)
    private val cache = Cache(context)

    val hasBarometer: Boolean
        get() = sensorChecker.hasBarometer()

    val shouldMonitorWeather: Boolean
        get() = sensorChecker.hasBarometer() && (cache.getBoolean(context.getString(R.string.pref_monitor_weather)) ?: true)

    val weatherUpdateFrequency: Duration
        get() {
            val raw = cache.getString(context.getString(R.string.pref_weather_update_frequency)) ?: "15"
            return Duration.ofMinutes(raw.toLongOrNull() ?: 15)
        }

    val shouldShowWeatherNotification: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_weather_notification)) ?: true

    val shouldShowPressureInNotification: Boolean
        get() = cache.getBoolean(
            context.getString(R.string.pref_show_pressure_in_notification)) ?:
            false

    val useSeaLevelPressure: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_use_sea_level_pressure)) ?: true

    val seaLevelFactorInTemp: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_adjust_for_temperature)) ?: false

    val seaLevelFactorInRapidChanges: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_sea_level_use_rapid)) ?: true

    val pressureHistory: Duration
        get() {
            val raw = cache.getString(context.getString(R.string.pref_pressure_history)) ?: "48"
            return Duration.ofHours(raw.toLong())
        }

    val requireDwell: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_sea_level_require_dwell)) ?: false

    val maxNonTravellingAltitudeChange: Float
        get() = cache.getInt(context.getString(R.string.pref_barometer_altitude_change))?.toFloat() ?: 60f

    val maxNonTravellingPressureChange: Float
        get() = 20 * (cache.getInt(context.getString(R.string.pref_sea_level_pressure_change_thresh))?.toFloat() ?: 50f) / 200f

    val sendStormAlerts: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_send_storm_alert)) ?: true

    val dailyForecastChangeThreshold: Float
        get() {
            return when (cache.getString(context.getString(R.string.pref_forecast_sensitivity))) {
                "low" -> 0.75f
                "high" -> 0.3f
                else -> 0.5f
            }
        }

    val hourlyForecastChangeThreshold: Float
        get() {
            return when (cache.getString(context.getString(R.string.pref_forecast_sensitivity))) {
                "low" -> 2.5f
                "high" -> 0.5f
                else -> 1.5f
            }
        }

    val stormAlertThreshold: Float
        get() {
            if (!sendStormAlerts) {
                return -4.5f
            }

            return when (cache.getString(context.getString(R.string.pref_storm_alert_sensitivity))) {
                "low" -> -6f
                "high" -> -3f
                else -> -4.5f
            }
        }

    var minBatteryTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_min_uncalibrated_temp_c))?.toFloatCompat2() ?: 0f
        set(value) {
            cache.putString(context.getString(R.string.pref_min_uncalibrated_temp_c), value.toString())
        }

    var minActualTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_min_calibrated_temp_c))?.toFloatCompat2() ?: 0f
        set(value) {
            cache.putString(context.getString(R.string.pref_min_calibrated_temp_c), value.toString())
        }

    var maxBatteryTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_max_uncalibrated_temp_c))?.toFloatCompat2() ?: 100f
        set(value) {
            cache.putString(context.getString(R.string.pref_max_uncalibrated_temp_c), value.toString())
        }

    var maxActualTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_max_calibrated_temp_c))?.toFloatCompat2() ?: 100f
        set(value) {
            cache.putString(context.getString(R.string.pref_max_calibrated_temp_c), value.toString())
        }

    var minBatteryTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_min_uncalibrated_temp_f))?.toFloatCompat2() ?: 32f
        set(value) {
            cache.putString(context.getString(R.string.pref_min_uncalibrated_temp_f), value.toString())
        }

    var minActualTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_min_calibrated_temp_f))?.toFloatCompat2() ?: 32f
        set(value) {
            cache.putString(context.getString(R.string.pref_min_calibrated_temp_f), value.toString())
        }

    var maxBatteryTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_max_uncalibrated_temp_f))?.toFloatCompat2() ?: 212f
        set(value) {
            cache.putString(context.getString(R.string.pref_max_uncalibrated_temp_f), value.toString())
        }

    var maxActualTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_max_calibrated_temp_f))?.toFloatCompat2() ?: 212f
        set(value) {
            cache.putString(context.getString(R.string.pref_max_calibrated_temp_f), value.toString())
        }

    val useLawOfCooling: Boolean
        get() = false

    val lawOfCoolingReadings: Int
        get() = 180

    val lawOfCoolingReadingInterval: Long
        get() = 500L

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