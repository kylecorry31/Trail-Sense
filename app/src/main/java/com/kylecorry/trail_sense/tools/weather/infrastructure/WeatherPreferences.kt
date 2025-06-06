package com.kylecorry.trail_sense.tools.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.toIntCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.forecast.ForecastSource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class WeatherPreferences(private val context: Context) : IWeatherPreferences {

    private val cache = PreferencesSubsystem.getInstance(context).preferences

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
                ?: 150) / 1000f
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

    override val shouldShowPressureInNotification: Boolean
        get() = cache.getBoolean(
            context.getString(R.string.pref_show_pressure_in_notification)
        ) ?: false

    override val shouldShowTemperatureInNotification: Boolean
        get() = cache.getBoolean(
            context.getString(R.string.pref_show_temperature_in_notification)
        ) ?: true

    override val useSeaLevelPressure: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_use_sea_level_pressure)) ?: true

    override val seaLevelFactorInTemp: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_adjust_for_temperature)) ?: false

    override var barometerOffset by FloatPreference(
        cache,
        context.getString(R.string.pref_barometer_offset),
        0f
    )

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

    override val leftButton: Int
        get() {
            val id = cache.getString(context.getString(R.string.pref_weather_quick_action_left))
                ?.toIntCompat()
            return id ?: (Tools.CLOUDS.toInt() + Tools.TOOL_QUICK_ACTION_OFFSET)
        }

    override val rightButton: Int
        get() {
            val id = cache.getString(context.getString(R.string.pref_weather_quick_action_right))
                ?.toIntCompat()
            return id ?: (Tools.TEMPERATURE_ESTIMATION.toInt() + Tools.TOOL_QUICK_ACTION_OFFSET)
        }

    override val showColoredNotificationIcon: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_weather_show_detailed_icon))
            ?: true

    override val forecastSource: ForecastSource by StringEnumPreference(
        cache,
        context.getString(R.string.pref_weather_forecast_source),
        mapOf(
            "1" to ForecastSource.Sol,
            "2" to ForecastSource.Zambretti
        ),
        ForecastSource.Sol
    )
    override val useAlarmForStormAlert: Boolean
        get() {
            val hours = stormAlertAlarmHours ?: return false
            val now = LocalTime.now()
            return hours.contains(now)
        }

    private val useAlarmForStormAlertInternal by BooleanPreference(
        cache,
        context.getString(R.string.pref_weather_use_alarm_for_storm_alert),
        false
    )

    private val muteStormAlarmAtNight by BooleanPreference(
        cache,
        context.getString(R.string.pref_weather_mute_storm_alarm_at_night),
        true
    )

    private val stormAlertAlarmHours: Range<LocalTime>?
        get() {
            if (!useAlarmForStormAlertInternal) {
                return null
            }

            return if (muteStormAlarmAtNight) {
                Range(LocalTime.of(8, 0), LocalTime.of(20, 0))
            } else {
                Range(LocalTime.MIN, LocalTime.MAX)
            }
        }

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