package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.cache.MemoryCachedValue
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.Weather
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

    private var cachedValue = MemoryCachedValue<ForecastCache>()

    init {
        resetWeatherService()
    }

    suspend fun getTendency(): PressureTendency {
        return withContext(Dispatchers.IO) {
            cachedValue.getOrPut { populateCache() }.tendency
        }
    }

    suspend fun getLastReading(): PressureReading? {
        return withContext(Dispatchers.IO) {
            cachedValue.getOrPut { populateCache() }.lastPressure
        }
    }

    suspend fun getHourlyForecast(): Weather {
        return withContext(Dispatchers.IO) {
            cachedValue.getOrPut { populateCache() }.hourly
        }
    }

    suspend fun getDailyForecast(): Weather {
        return withContext(Dispatchers.IO) {
            cachedValue.getOrPut { populateCache() }.daily
        }
    }

    suspend fun getPressureHistory(): List<PressureReading> {
        val readings = weatherRepo.getPressuresSync()
            .map { it.toPressureAltitudeReading() }
            .sortedBy { it.time }
            .filter { it.time <= Instant.now() }

        val calibrator = SeaLevelCalibrationFactory().create(prefs)
        return calibrator.calibrate(readings)
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

    private fun getSetpoint(): PressureReading? {
        val setpoint = prefs.weather.pressureSetpoint
        return if (prefs.weather.useSeaLevelPressure) {
            setpoint?.seaLevel(prefs.weather.seaLevelFactorInTemp)
        } else {
            setpoint?.pressureReading()
        }
    }

    private fun getHourlyForecast(readings: List<PressureReading>): Weather {
        return weatherService.getHourlyWeather(readings, getSetpoint())
    }

    private fun getDailyForecast(readings: List<PressureReading>): Weather {
        return weatherService.getDailyWeather(readings)
    }

    private suspend fun populateCache(): ForecastCache {
        val readings = getPressureHistory()
        val daily = getDailyForecast(readings)
        val hourly = getHourlyForecast(readings)
        val last = readings.lastOrNull()
        val tendency = weatherService.getTendency(readings, getSetpoint())
        return ForecastCache(hourly, daily, tendency, last)
    }

    private fun resetWeatherService() {
        weatherService = WeatherService(
            prefs.weather.stormAlertThreshold,
            prefs.weather.dailyForecastChangeThreshold,
            prefs.weather.hourlyForecastChangeThreshold
        )
    }

    private data class ForecastCache(
        val hourly: Weather,
        val daily: Weather,
        val tendency: PressureTendency,
        val lastPressure: PressureReading?
    )

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