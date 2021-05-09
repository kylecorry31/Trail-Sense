package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionType
import com.kylecorry.trailsensecore.domain.math.toFloatCompat
import com.kylecorry.trailsensecore.domain.math.toIntCompat
import com.kylecorry.trailsensecore.domain.weather.ISeaLevelPressureConverter
import com.kylecorry.trailsensecore.domain.weather.KalmanSeaLevelPressureConverter
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.pow

class WeatherPreferences(private val context: Context) {

    private val sensorChecker = SensorChecker(context)
    private val cache = Cache(context)

    val hasBarometer: Boolean
        get() = sensorChecker.hasBarometer()

    var shouldMonitorWeather: Boolean
        get() = sensorChecker.hasBarometer() && (cache.getBoolean(context.getString(R.string.pref_monitor_weather))
            ?: true)
        set(value) {
            cache.putBoolean(context.getString(R.string.pref_monitor_weather), value)
        }

    val useExperimentalCalibration: Boolean
        get() {
            val experimental = cache.getBoolean(context.getString(R.string.pref_enable_experimental)) ?: false
            if (!experimental){
                return false
            }
            return cache.getBoolean(context.getString(R.string.pref_experimental_barometer_calibration)) ?: false
        }

    val experimentalConverter: ISeaLevelPressureConverter?
        get() {
            if (!useExperimentalCalibration){
                return null
            }
            return KalmanSeaLevelPressureConverter(
                altitudeOutlierThreshold = altitudeOutlier,
                defaultGPSError = if (useAltitudeVariance) 34f.pow(2) else 34f,
                defaultPressureError = 1f,
                pressureProcessError = (1 - pressureSmoothing / 100f).pow(4) * 0.1f,
                altitudeProcessError = (1 - altitudeSmoothing / 100f).pow(4) * 10f,
                adjustWithTime = true,
                replaceLastOutlier = true
            )
        }

    val useAltitudeVariance: Boolean = true

    val altitudeOutlier: Float
        get() = cache.getInt(context.getString(R.string.pref_barometer_altitude_outlier))?.toFloat() ?: 34f

    val pressureSmoothing: Float
        get(){
            val raw = (cache.getInt(context.getString(R.string.pref_barometer_pressure_smoothing)) ?: 500) / 1000f
            return raw * 100
        }

    val altitudeSmoothing: Float
        get(){
            val raw = (cache.getInt(context.getString(R.string.pref_barometer_altitude_smoothing)) ?: 0) / 1000f
            return raw * 100
        }

    var weatherUpdateFrequency: Duration
        get() {
            val raw =
                cache.getString(context.getString(R.string.pref_weather_update_frequency)) ?: "15"
            return Duration.ofMinutes(raw.toLongOrNull() ?: 15)
        }
        set(value) {
            cache.putString(context.getString(R.string.pref_weather_update_frequency), value.toMinutes().toString())
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
        get(){
            val raw = (cache.getString(context.getString(R.string.pref_daily_weather_time)) ?: LocalTime.of(7, 0).toString())
            return LocalTime.parse(raw)
        }
        set(value) {
            cache.putString(context.getString(R.string.pref_daily_weather_time), value.toString())
        }

    val leftQuickAction: QuickActionType
        get(){
            val id = cache.getString(context.getString(R.string.pref_weather_quick_action_left))?.toIntCompat()
            return QuickActionType.values().firstOrNull { it.id == id } ?: QuickActionType.Clouds
        }

    val rightQuickAction: QuickActionType
        get(){
            val id = cache.getString(context.getString(R.string.pref_weather_quick_action_right))?.toIntCompat()
            return QuickActionType.values().firstOrNull { it.id == id } ?: QuickActionType.Temperature
        }

    val showColoredNotificationIcon: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_weather_show_detailed_icon)) ?: true

}