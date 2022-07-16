package com.kylecorry.trail_sense.weather.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.cache.MemoryCachedValue
import com.kylecorry.andromeda.core.topics.ITopic
import com.kylecorry.andromeda.core.topics.Topic
import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.domain.sealevel.SeaLevelCalibrationFactory
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import kotlinx.coroutines.delay
import java.time.Instant

class WeatherSubsystem private constructor(private val context: Context) {

    // TODO: Reset when any of the weather preferences changes regarding tendencies, sea level pressure and calibration

    private val weatherRepo by lazy { PressureRepo.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }

    private lateinit var weatherService: WeatherService

    private var cachedValue = MemoryCachedValue<CurrentWeather>()

    // TODO: Emit weather as payload of topic
    private val _weatherChanged = Topic()
    val weatherChanged: ITopic = _weatherChanged

    init {
        resetWeatherService()
    }

    suspend fun getWeather(): CurrentWeather {
        return onIO {
            cachedValue.getOrPut { populateCache() }
        }
    }

    suspend fun getHistory(): List<WeatherObservation> {
        val readings = weatherRepo.getPressuresSync()
            .asSequence()
            .map { it.toPressureAltitudeReading() }
            .sortedBy { it.time }
            .filter { it.time <= Instant.now() }
            .toList()

        val calibrator = SeaLevelCalibrationFactory().create(prefs)
        val pressures = calibrator.calibrate(readings)

        return pressures.map {
            val reading = readings.firstOrNull { r -> r.time == it.time }
            WeatherObservation(
                it.time,
                Pressure.hpa(it.value),
                Temperature.celsius(
                    weatherService.calibrateTemperature(reading?.temperature ?: 0f)
                ),
                reading?.humidity
            )
        }
    }

    suspend fun invalidate() {
        cachedValue.reset()
        delay(50)
        resetWeatherService()
        _weatherChanged.notifySubscribers()
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