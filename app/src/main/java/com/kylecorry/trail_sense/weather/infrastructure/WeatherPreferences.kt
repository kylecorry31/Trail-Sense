package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.math.toFloatCompat
import com.kylecorry.andromeda.core.math.toIntCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionType
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class WeatherPreferences(private val context: Context) {

    private val cache = Preferences(context)

    val hasBarometer: Boolean
        get() = Sensors.hasBarometer(context)

    var shouldMonitorWeather: Boolean
        get() = Sensors.hasBarometer(context) && (cache.getBoolean(context.getString(R.string.pref_monitor_weather))
            ?: true)
        set(value) {
            cache.putBoolean(context.getString(R.string.pref_monitor_weather), value)
        }

    val useExperimentalCalibration by BooleanPreference(
        cache,
        context.getString(R.string.pref_experimental_barometer_calibration),
        false
    )

    val useAltitudeVariance: Boolean = true

    val altitudeOutlier: Float
        get() = cache.getInt(context.getString(R.string.pref_barometer_altitude_outlier))?.toFloat()
            ?: 34f

    val pressureSmoothing: Float
        get() {
            val raw = (cache.getInt(context.getString(R.string.pref_barometer_pressure_smoothing))
                ?: 500) / 1000f
            return raw * 100
        }

    val altitudeSmoothing: Float
        get() {
            val raw = (cache.getInt(context.getString(R.string.pref_barometer_altitude_smoothing))
                ?: 0) / 1000f
            return raw * 100
        }

    var weatherUpdateFrequency: Duration
        get() {
            val raw =
                cache.getString(context.getString(R.string.pref_weather_update_frequency)) ?: "15"
            return Duration.ofMinutes(raw.toLongOrNull() ?: 15)
        }
        set(value) {
            cache.putString(
                context.getString(R.string.pref_weather_update_frequency),
                value.toMinutes().toString()
            )
        }

    val shouldShowDailyWeatherNotification: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_daily_weather_notification))
            ?: true

    val shouldShowWeatherNotification: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_weather_notification)) ?: true

    val shouldShowPressureInNotification: Boolean
        get() = cache.getBoolean(
            context.getString(R.string.pref_show_pressure_in_notification)
        ) ?: false

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
        get() = cache.getInt(context.getString(R.string.pref_barometer_altitude_change))?.toFloat()
            ?: 60f

    val maxNonTravellingPressureChange: Float
        get() = 20 * (cache.getInt(context.getString(R.string.pref_sea_level_pressure_change_thresh))
            ?.toFloat() ?: 50f) / 200f

    val sendStormAlerts: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_send_storm_alert)) ?: true

    val dailyForecastChangeThreshold: Float
        get() {
            return when (cache.getString(context.getString(R.string.pref_forecast_sensitivity))) {
                "low" -> HPA_DAILY_LOW
                "high" -> HPA_DAILY_HIGH
                else -> HPA_DAILY_MEDIUM
            }
        }

    val hourlyForecastChangeThreshold: Float
        get() {
            return when (cache.getString(context.getString(R.string.pref_forecast_sensitivity))) {
                "low" -> HPA_FORECAST_LOW
                "high" -> HPA_FORECAST_HIGH
                else -> HPA_FORECAST_MEDIUM
            }
        }

    val stormAlertThreshold: Float
        get() {
            if (!sendStormAlerts) {
                return HPA_STORM_MEDIUM
            }

            return when (cache.getString(context.getString(R.string.pref_storm_alert_sensitivity))) {
                "low" -> HPA_STORM_LOW
                "high" -> HPA_STORM_HIGH
                else -> HPA_STORM_MEDIUM
            }
        }

    val useRelativeWeatherPredictions: Boolean
        get() = true

    var minBatteryTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_min_uncalibrated_temp_c))
            ?.toFloatCompat() ?: 0f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_min_uncalibrated_temp_c),
                value.toString()
            )
        }

    var minActualTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_min_calibrated_temp_c))
            ?.toFloatCompat() ?: 0f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_min_calibrated_temp_c),
                value.toString()
            )
        }

    var maxBatteryTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_max_uncalibrated_temp_c))
            ?.toFloatCompat() ?: 100f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_max_uncalibrated_temp_c),
                value.toString()
            )
        }

    var maxActualTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_max_calibrated_temp_c))
            ?.toFloatCompat() ?: 100f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_max_calibrated_temp_c),
                value.toString()
            )
        }

    var minBatteryTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_min_uncalibrated_temp_f))
            ?.toFloatCompat() ?: 32f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_min_uncalibrated_temp_f),
                value.toString()
            )
        }

    var minActualTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_min_calibrated_temp_f))
            ?.toFloatCompat() ?: 32f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_min_calibrated_temp_f),
                value.toString()
            )
        }

    var maxBatteryTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_max_uncalibrated_temp_f))
            ?.toFloatCompat() ?: 212f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_max_uncalibrated_temp_f),
                value.toString()
            )
        }

    var maxActualTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_max_calibrated_temp_f))
            ?.toFloatCompat() ?: 212f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_max_calibrated_temp_f),
                value.toString()
            )
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

    var dailyWeatherLastSent: LocalDate
        get() {
            val raw = (cache.getString("daily_weather_last_sent_date") ?: LocalDate.MIN.toString())
            return LocalDate.parse(raw)
        }
        set(value) {
            cache.putString("daily_weather_last_sent_date", value.toString())
        }

    val dailyWeatherIsForTomorrow: Boolean
        get() = dailyForecastTime >= LocalTime.of(16, 0)

    var dailyForecastTime: LocalTime
        get() {
            val raw = (cache.getString(context.getString(R.string.pref_daily_weather_time))
                ?: LocalTime.of(7, 0).toString())
            return LocalTime.parse(raw)
        }
        set(value) {
            cache.putString(context.getString(R.string.pref_daily_weather_time), value.toString())
        }

    val leftQuickAction: QuickActionType
        get() {
            val id = cache.getString(context.getString(R.string.pref_weather_quick_action_left))
                ?.toIntCompat()
            return QuickActionType.values().firstOrNull { it.id == id } ?: QuickActionType.Clouds
        }

    val rightQuickAction: QuickActionType
        get() {
            val id = cache.getString(context.getString(R.string.pref_weather_quick_action_right))
                ?.toIntCompat()
            return QuickActionType.values().firstOrNull { it.id == id }
                ?: QuickActionType.Temperature
        }

    val showColoredNotificationIcon: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_weather_show_detailed_icon))
            ?: true


    companion object {
        const val HPA_FORECAST_LOW = 2.5f
        const val HPA_FORECAST_MEDIUM = 1.5f
        const val HPA_FORECAST_HIGH = 0.5f

        const val HPA_DAILY_LOW = 0.75f
        const val HPA_DAILY_MEDIUM = 0.5f
        const val HPA_DAILY_HIGH = 0.3f

        const val HPA_STORM_LOW = -6f
        const val HPA_STORM_MEDIUM = -4.5f
        const val HPA_STORM_HIGH = -3f
    }

}