package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.toFloatCompat
import com.kylecorry.andromeda.core.toIntCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionType
import com.kylecorry.trail_sense.shared.extensions.getDuration
import com.kylecorry.trail_sense.shared.extensions.putDuration
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class WeatherPreferences(private val context: Context) : IWeatherPreferences {

    private val cache = Preferences(context)

    override val hasBarometer: Boolean
        get() = Sensors.hasBarometer(context)

    override var shouldMonitorWeather: Boolean
        get() = Sensors.hasBarometer(context) && (cache.getBoolean(context.getString(R.string.pref_monitor_weather))
            ?: false)
        set(value) {
            cache.putBoolean(context.getString(R.string.pref_monitor_weather), value)
        }

    override var pressureSmoothing: Float
        get() {
            val raw = (cache.getInt(context.getString(R.string.pref_barometer_pressure_smoothing))
                ?: 300) / 1000f
            return raw * 100
        }
        set(value) {
            val scaled = (value * 10).coerceIn(0f, 1000f)
            cache.putInt(
                context.getString(R.string.pref_barometer_pressure_smoothing),
                scaled.toInt()
            )
        }

    override var weatherUpdateFrequency: Duration
        get() {
            return cache.getDuration(context.getString(R.string.pref_weather_update_frequency))
                ?: Duration.ofMinutes(15)
        }
        set(value) {
            cache.putDuration(context.getString(R.string.pref_weather_update_frequency), value)
        }

    override val shouldShowDailyWeatherNotification: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_daily_weather_notification))
            ?: true

    override val shouldShowWeatherNotification: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_weather_notification)) ?: true

    override val shouldShowPressureInNotification: Boolean
        get() = cache.getBoolean(
            context.getString(R.string.pref_show_pressure_in_notification)
        ) ?: false

    override val useSeaLevelPressure: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_use_sea_level_pressure)) ?: true

    override var showMeanShiftedReadings by BooleanPreference(
        cache,
        context.getString(R.string.pref_debug_show_mean_adj_sea_level),
        false
    )

    override val seaLevelFactorInTemp: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_adjust_for_temperature)) ?: false

    override val pressureHistory: Duration
        get() {
            val raw = cache.getString(context.getString(R.string.pref_pressure_history)) ?: "48"
            return Duration.ofHours(raw.toLong())
        }

    override val sendStormAlerts: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_send_storm_alert)) ?: true

    override val dailyForecastChangeThreshold: Float
        get() {
            return when (cache.getString(context.getString(R.string.pref_forecast_sensitivity))) {
                "low" -> HPA_DAILY_LOW
                "high" -> HPA_DAILY_HIGH
                else -> HPA_DAILY_MEDIUM
            }
        }

    override val hourlyForecastChangeThreshold: Float
        get() {
            return when (cache.getString(context.getString(R.string.pref_forecast_sensitivity))) {
                "low" -> HPA_FORECAST_LOW
                "high" -> HPA_FORECAST_HIGH
                else -> HPA_FORECAST_MEDIUM
            }
        }

    override val stormAlertThreshold: Float
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

    override var minBatteryTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_min_uncalibrated_temp_c))
            ?.toFloatCompat() ?: 0f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_min_uncalibrated_temp_c),
                value.toString()
            )
        }

    override var minActualTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_min_calibrated_temp_c))
            ?.toFloatCompat() ?: 0f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_min_calibrated_temp_c),
                value.toString()
            )
        }

    override var maxBatteryTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_max_uncalibrated_temp_c))
            ?.toFloatCompat() ?: 100f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_max_uncalibrated_temp_c),
                value.toString()
            )
        }

    override var maxActualTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_max_calibrated_temp_c))
            ?.toFloatCompat() ?: 100f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_max_calibrated_temp_c),
                value.toString()
            )
        }

    override var minBatteryTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_min_uncalibrated_temp_f))
            ?.toFloatCompat() ?: 32f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_min_uncalibrated_temp_f),
                value.toString()
            )
        }

    override var minActualTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_min_calibrated_temp_f))
            ?.toFloatCompat() ?: 32f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_min_calibrated_temp_f),
                value.toString()
            )
        }

    override var maxBatteryTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_max_uncalibrated_temp_f))
            ?.toFloatCompat() ?: 212f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_max_uncalibrated_temp_f),
                value.toString()
            )
        }

    override var maxActualTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_max_calibrated_temp_f))
            ?.toFloatCompat() ?: 212f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_max_calibrated_temp_f),
                value.toString()
            )
        }

    override var dailyWeatherLastSent: LocalDate
        get() {
            val raw = (cache.getString("daily_weather_last_sent_date") ?: LocalDate.MIN.toString())
            return LocalDate.parse(raw)
        }
        set(value) {
            cache.putString("daily_weather_last_sent_date", value.toString())
        }

    override val dailyWeatherIsForTomorrow: Boolean
        get() = dailyForecastTime >= LocalTime.of(16, 0)

    override var dailyForecastTime: LocalTime
        get() {
            val raw = (cache.getString(context.getString(R.string.pref_daily_weather_time))
                ?: LocalTime.of(7, 0).toString())
            return LocalTime.parse(raw)
        }
        set(value) {
            cache.putString(context.getString(R.string.pref_daily_weather_time), value.toString())
        }

    override val leftButton: QuickActionType
        get() {
            val id = cache.getString(context.getString(R.string.pref_weather_quick_action_left))
                ?.toIntCompat()
            return QuickActionType.values().firstOrNull { it.id == id } ?: QuickActionType.Clouds
        }

    override val rightButton: QuickActionType
        get() {
            val id = cache.getString(context.getString(R.string.pref_weather_quick_action_right))
                ?.toIntCompat()
            return QuickActionType.values().firstOrNull { it.id == id }
                ?: QuickActionType.Temperature
        }

    override val showColoredNotificationIcon: Boolean
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