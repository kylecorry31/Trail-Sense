package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.isInPast
import com.kylecorry.trail_sense.shared.isOlderThan
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import com.kylecorry.trailsensecore.domain.weather.PressureReading
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

    // TODO: Allow this to be reset
    private val weatherService by lazy {
        WeatherService(
            prefs.weather.stormAlertThreshold,
            prefs.weather.dailyForecastChangeThreshold,
            prefs.weather.hourlyForecastChangeThreshold,
            prefs.weather.seaLevelFactorInRapidChanges,
            prefs.weather.seaLevelFactorInTemp
        )
    }

    private var cache: List<PressureReading>? = null
    private var cacheTime = Instant.MIN

    private val mutex = Mutex()

    // TODO: Add weather

    suspend fun getHourlyForecast(): Weather {
        return withContext(Dispatchers.IO) {
            getHourlyForecast(getReadings())
        }
    }

    suspend fun getDailyForecast(): Weather {
        return withContext(Dispatchers.IO) {
            getDailyForecast(getReadings())
        }
    }

    suspend fun setDataChanged() {
        mutex.withLock {
            cacheTime = null
            cache = null
        }
    }

    private fun getHourlyForecast(readings: List<PressureReading>): Weather {
        return weatherService.getHourlyWeather(readings)
    }

    private fun getDailyForecast(readings: List<PressureReading>): Weather {
        return weatherService.getDailyWeather(readings)
    }


    private suspend fun getReadings(): List<PressureReading> {
        return mutex.withLock {
            if (hasValidCache()) {
                return cache ?: listOf()
            }

            val readings = weatherRepo.getPressuresSync().map { it.toPressureAltitudeReading() }
            val calibrated = PressureCalibrationUtils.calibratePressures(context, readings)
            cache = calibrated
            cacheTime = Instant.now()
            calibrated
        }
    }

    private fun hasValidCache(): Boolean {
        return cache != null
                && !cacheTime.isOlderThan(MAX_CACHE_TIME)
                && cacheTime.isInPast()
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