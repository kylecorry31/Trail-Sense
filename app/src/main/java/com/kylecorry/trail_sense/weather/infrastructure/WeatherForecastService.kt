package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.isInPast
import com.kylecorry.trail_sense.shared.isOlderThan
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import com.kylecorry.trailsensecore.domain.weather.PressureReading
import com.kylecorry.trailsensecore.domain.weather.PressureTendency
import com.kylecorry.trailsensecore.domain.weather.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

class WeatherForecastService(private val context: Context) {

    private val weatherRepo by lazy { PressureRepo.getInstance(context) }
    private val prefs by lazy { UserPreferences(context) }

    private lateinit var weatherService: WeatherService

    private var cacheHourly: Weather? = null
    private var cacheDaily: Weather? = null
    private var cacheLastReading: PressureReading? = null
    private var cacheTendency: PressureTendency? = null
    private var cacheTime = Instant.MIN

    private val mutex = Mutex()

    init {
        resetWeatherService()
    }

    suspend fun getTendency(): PressureTendency {
        return withContext(Dispatchers.IO) {
            mutex.withLock(this@WeatherForecastService) {
                if (!hasValidCache()){
                    populateCache()
                }
                cacheTendency!!
            }
        }
    }

    suspend fun getLastReading(): PressureReading? {
        return withContext(Dispatchers.IO) {
            mutex.withLock(this@WeatherForecastService) {
                if (!hasValidCache()){
                    populateCache()
                }
                cacheLastReading
            }
        }
    }

    suspend fun getHourlyForecast(): Weather {
        return withContext(Dispatchers.IO) {
            mutex.withLock(this@WeatherForecastService) {
                if (!hasValidCache()){
                    populateCache()
                }
                cacheHourly!!
            }
        }
    }

    suspend fun getDailyForecast(): Weather {
        return withContext(Dispatchers.IO) {
            mutex.withLock(this@WeatherForecastService) {
                if (!hasValidCache()){
                    populateCache()
                }
                cacheDaily!!
            }
        }
    }

    suspend fun setDataChanged() {
        mutex.withLock(this@WeatherForecastService) {
            cacheTime = null
            cacheHourly = null
            cacheDaily = null
            resetWeatherService()
        }
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

    private suspend fun populateCache(){
        val readings = getReadings()
        cacheDaily = getDailyForecast(readings)
        cacheHourly = getHourlyForecast(readings)
        cacheLastReading = readings.lastOrNull()
        cacheTendency = weatherService.getTendency(readings, getSetpoint())
        cacheTime = Instant.now()
    }

    private suspend fun getReadings(): List<PressureReading> {
        val readings = weatherRepo.getPressuresSync().map { it.toPressureAltitudeReading() }
        return PressureCalibrationUtils.calibratePressures(context, readings)
    }

    private fun hasValidCache(): Boolean {
        return cacheDaily != null
                && cacheHourly != null
                && cacheTendency != null
                && !cacheTime.isOlderThan(MAX_CACHE_TIME)
                && cacheTime.isInPast()
    }

    private fun resetWeatherService(){
        weatherService = WeatherService(
            prefs.weather.stormAlertThreshold,
            prefs.weather.dailyForecastChangeThreshold,
            prefs.weather.hourlyForecastChangeThreshold,
            prefs.weather.seaLevelFactorInRapidChanges,
            prefs.weather.seaLevelFactorInTemp
        )
    }


    companion object {

        private val MAX_CACHE_TIME = Duration.ofMinutes(5)

        private var instance: WeatherForecastService? = null

        @Synchronized
        fun getInstance(context: Context): WeatherForecastService {
            if (instance == null) {
                instance = WeatherForecastService(context.applicationContext)
            }
            return instance!!
        }

    }

}