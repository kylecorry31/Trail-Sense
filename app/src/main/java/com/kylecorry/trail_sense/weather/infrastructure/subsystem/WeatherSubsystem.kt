package com.kylecorry.trail_sense.weather.infrastructure.subsystem

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.cache.MemoryCachedValue
import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.andromeda.core.topics.Topic
import com.kylecorry.andromeda.core.topics.generic.distinct
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.domain.sealevel.SeaLevelCalibrationFactory
import com.kylecorry.trail_sense.weather.infrastructure.*
import com.kylecorry.trail_sense.weather.infrastructure.commands.MonitorWeatherCommand
import com.kylecorry.trail_sense.weather.infrastructure.persistence.WeatherRepo
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.util.*


class WeatherSubsystem private constructor(private val context: Context) : IWeatherSubsystem {

    private val weatherRepo by lazy { WeatherRepo.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }
    private val sharedPrefs by lazy { Preferences(context) }

    private lateinit var weatherService: WeatherService

    private var cachedValue = MemoryCachedValue<CurrentWeather>()
    private var validLock = Object()
    private var isValid = false

    private val _weatherChanged = Topic()
    override val weatherChanged: ITopic = _weatherChanged

    private val _weatherMonitorStateChanged =
        com.kylecorry.andromeda.core.topics.generic.Topic(defaultValue = Optional.of(calculateWeatherMonitorState()))
    override val weatherMonitorStateChanged: com.kylecorry.andromeda.core.topics.generic.ITopic<FeatureState>
        get() = _weatherMonitorStateChanged.distinct()

    private val _weatherMonitorFrequencyChanged =
        com.kylecorry.andromeda.core.topics.generic.Topic(defaultValue = Optional.of(calculateWeatherMonitorFrequency()))
    override val weatherMonitorFrequencyChanged: com.kylecorry.andromeda.core.topics.generic.ITopic<Duration>
        get() = _weatherMonitorFrequencyChanged.distinct()

    private val invalidationPrefKeys = listOf(
        R.string.pref_use_sea_level_pressure,
        R.string.pref_barometer_altitude_outlier,
        R.string.pref_barometer_pressure_smoothing,
        R.string.pref_barometer_altitude_smoothing,
        R.string.pref_adjust_for_temperature,
        R.string.pref_forecast_sensitivity,
        R.string.pref_storm_alert_sensitivity,
        R.string.pref_altimeter_calibration_mode,
        R.string.pref_pressure_history
    ).map { context.getString(it) }

    private val weatherMonitorStatePrefKeys = listOf(
        R.string.pref_monitor_weather,
        R.string.pref_low_power_mode_weather,
        R.string.pref_low_power_mode
    ).map { context.getString(it) }

    private val weatherMonitorFrequencyPrefKeys = listOf(
        R.string.pref_weather_update_frequency
    ).map { context.getString(it) }

    init {
        sharedPrefs.onChange.subscribe { key ->
            if (key in invalidationPrefKeys) {
                invalidate()
            }

            if (key in weatherMonitorStatePrefKeys) {
                val state = calculateWeatherMonitorState()
                _weatherMonitorStateChanged.notifySubscribers(state)
            }

            if (key in weatherMonitorFrequencyPrefKeys) {
                val frequency = calculateWeatherMonitorFrequency()
                _weatherMonitorFrequencyChanged.notifySubscribers(frequency)
            }

            true
        }
        weatherRepo.readingsChanged.subscribe {
            invalidate()
            true
        }
        resetWeatherService()
    }

    override suspend fun getWeather(): CurrentWeather = onIO {
        if (!isValid) {
            refresh()
        }
        cachedValue.getOrPut { populateCache() }
    }

    override suspend fun getHistory(): List<WeatherObservation> = onIO {
        if (!isValid) {
            refresh()
        }
        val readings = weatherRepo.getAll()
            .asSequence()
            .sortedBy { it.time }
            .filter { it.time <= Instant.now() }
            .toList()

        val calibrator = SeaLevelCalibrationFactory().create(prefs)
        val pressures = calibrator.calibrate(readings)

        pressures.map {
            val reading = readings.firstOrNull { r -> r.time == it.time }
            WeatherObservation(
                it.time,
                it.value,
                Temperature.celsius(
                    weatherService.calibrateTemperature(reading?.value?.temperature ?: 0f)
                ),
                reading?.value?.humidity
            )
        }
    }

    override fun enableMonitor() {
        prefs.weather.shouldMonitorWeather = true
        WeatherUpdateScheduler.start(context)
    }

    override fun disableMonitor() {
        prefs.weather.shouldMonitorWeather = false
        WeatherUpdateScheduler.stop(context)
    }

    override suspend fun updateWeather(background: Boolean) {
        MonitorWeatherCommand(context, background).execute()
    }

    private fun invalidate() {
        synchronized(validLock) {
            isValid = false
        }
        _weatherChanged.notifySubscribers()
    }

    private suspend fun refresh() {
        cachedValue.reset()
        delay(50)
        resetWeatherService()
        synchronized(validLock) {
            isValid = true
        }
    }

    private fun getHourlyForecast(readings: List<WeatherObservation>): Weather {
        return weatherService.getHourlyWeather(readings.map { it.pressureReading() })
    }

    private fun getDailyForecast(readings: List<WeatherObservation>): Weather {
        return weatherService.getDailyWeather(readings.map { it.pressureReading() })
    }

    private suspend fun populateCache(): CurrentWeather {
        val history = getHistory()
        val daily = getDailyForecast(history)
        val hourly = getHourlyForecast(history)
        val last = history.lastOrNull()
        val tendency = weatherService.getTendency(history.map { it.pressureReading() })
        return CurrentWeather(
            WeatherPrediction(hourly, daily),
            tendency,
            last
        )
    }

    private fun resetWeatherService() {
        weatherService = WeatherService(prefs.weather)
    }

    private fun calculateWeatherMonitorFrequency(): Duration {
        return prefs.weather.weatherUpdateFrequency
    }

    private fun calculateWeatherMonitorState(): FeatureState {
        return if (WeatherMonitorIsEnabled().isSatisfiedBy(context)) {
            FeatureState.On
        } else if (WeatherMonitorIsAvailable().not().isSatisfiedBy(context)) {
            FeatureState.Unavailable
        } else {
            FeatureState.Off
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: WeatherSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): WeatherSubsystem {
            if (instance == null) {
                instance = WeatherSubsystem(context.applicationContext)
            }
            return instance!!
        }

    }

}