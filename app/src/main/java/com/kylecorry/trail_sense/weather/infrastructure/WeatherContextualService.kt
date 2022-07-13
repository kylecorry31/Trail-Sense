package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.cache.MemoryCachedValue
import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureReading
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.domain.sealevel.SeaLevelCalibrationFactory
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant

class WeatherContextualService private constructor(private val context: Context) {

    // TODO: Reset when any of the weather preferences changes regarding tendencies, sea level pressure and calibration

    private val weatherRepo by lazy { PressureRepo.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }

    private lateinit var weatherService: WeatherService

    private var cachedValue = MemoryCachedValue<CurrentWeather>()

    init {
        resetWeatherService()
    }

    suspend fun getWeather(): CurrentWeather {
        return withContext(Dispatchers.IO) {
            cachedValue.getOrPut { populateCache() }
        }
    }

    // TODO: Merge all histories using a WeatherReading object

    suspend fun getPressureHistory(): List<PressureReading> {
        val readings = weatherRepo.getPressuresSync()
            .asSequence()
            .map { it.toPressureAltitudeReading() }
            .sortedBy { it.time }
            .filter { it.time <= Instant.now() }
            .toList()

        val calibrator = SeaLevelCalibrationFactory().create(prefs)
        return calibrator.calibrate(readings)
    }

    suspend fun getTemperatureHistory(): List<Reading<Float>> {
        return weatherRepo.getPressuresSync()
            .asSequence()
            .map { it.toPressureAltitudeReading() }
            .map {
                val temperature = weatherService.calibrateTemperature(it.temperature)
                Reading(temperature, it.time)
            }
            .sortedBy { it.time }
            .filter { it.time <= Instant.now() }
            .toList()
    }

    suspend fun getHumidityHistory(): List<Reading<Float>> {
        return weatherRepo.getPressuresSync()
            .asSequence()
            .map { it.toPressureAltitudeReading() }
            .filter { it.humidity != null }
            .map {
                Reading(it.humidity!!, it.time)
            }
            .sortedBy { it.time }
            .filter { it.time <= Instant.now() }
            .toList()
    }

    fun getSeaLevelPressure(
        reading: PressureAltitudeReading,
        history: List<PressureAltitudeReading> = listOf()
    ): PressureReading {
        val calibrator = SeaLevelCalibrationFactory().create(prefs)
        val readings = calibrator.calibrate(history + listOf(reading))
        return readings.lastOrNull() ?: reading.seaLevel(prefs.weather.seaLevelFactorInTemp)
    }

    suspend fun setDataChanged() {
        cachedValue.reset()
        delay(50)
        resetWeatherService()
    }

    private fun getHourlyForecast(readings: List<PressureReading>): Weather {
        return weatherService.getHourlyWeather(readings)
    }

    private fun getDailyForecast(readings: List<PressureReading>): Weather {
        return weatherService.getDailyWeather(readings)
    }

    private suspend fun calculateLastTemperature(): Reading<Float>? {
        return getTemperatureHistory().lastOrNull()
    }

    private suspend fun calculateLastHumidity(): Reading<Float>? {
        return getHumidityHistory().lastOrNull()
    }

    private suspend fun populateCache(): CurrentWeather {
        val readings = getPressureHistory()
        val daily = getDailyForecast(readings)
        val hourly = getHourlyForecast(readings)
        val last = readings.lastOrNull()
        val tendency = weatherService.getTendency(readings)
        val lastTemperature = calculateLastTemperature()?.let { Reading(Temperature.celsius(it.value), it.time) }
        val lastHumidity = calculateLastHumidity()
        return CurrentWeather(hourly, daily, tendency, last, lastTemperature, lastHumidity)
    }

    private fun resetWeatherService() {
        weatherService = WeatherService(prefs.weather)
    }

    companion object {
        private var instance: WeatherContextualService? = null

        @Synchronized
        fun getInstance(context: Context): WeatherContextualService {
            if (instance == null) {
                instance = WeatherContextualService(context.applicationContext)
            }
            return instance!!
        }

    }

}