package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.core.sensors.read
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.weather.domain.PressureReading
import com.kylecorry.trail_sense.weather.infrastructure.WeatherContextualService
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureReadingEntity
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant

class MonitorWeatherCommand(private val context: Context) : CoroutineCommand {

    private val sensorService by lazy { SensorService(context) }
    private val altimeter by lazy { sensorService.getGPSAltimeter(true) }
    private val barometer by lazy { sensorService.getBarometer() }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val hygrometer by lazy { sensorService.getHygrometer() }
    private val pressureRepo by lazy { PressureRepo.getInstance(context) }
    private val weatherForecastService by lazy { WeatherContextualService.getInstance(context) }

    override suspend fun execute() {
        sendWeatherNotifications()

        withTimeoutOrNull(Duration.ofSeconds(30).toMillis()) {
            val jobs = mutableListOf<Job>()
            if (!altimeter.hasValidReading) {
                jobs.add(launch { altimeter.read() })
            }

            if (!barometer.hasValidReading) {
                jobs.add(launch { barometer.read() })
            }

            if (!thermometer.hasValidReading) {
                jobs.add(launch { thermometer.read() })
            }

            if (!hygrometer.hasValidReading) {
                jobs.add(launch { hygrometer.read() })
            }

            jobs.joinAll()
        }

        recordReading()
        sendWeatherNotifications()
    }


    private suspend fun recordReading() {
        if (barometer.pressure == 0f) {
            return
        }
        withContext(Dispatchers.IO) {
            pressureRepo.addPressure(
                PressureReadingEntity(
                    barometer.pressure,
                    altimeter.altitude,
                    if (altimeter is IGPS) ((altimeter as IGPS).verticalAccuracy ?: 0f) else 0f,
                    if (thermometer.temperature.isNaN()) 16f else thermometer.temperature,
                    hygrometer.humidity,
                    Instant.now().toEpochMilli()
                )
            )
            pressureRepo.deleteOlderThan(Instant.now().minus(Duration.ofDays(2)))
        }
    }

    private suspend fun sendWeatherNotifications() {

        var hourly: Weather
        var daily: Weather
        var tendency: PressureTendency
        var lastReading: PressureReading?
        withContext(Dispatchers.IO) {
            hourly = weatherForecastService.getHourlyForecast()
            daily = weatherForecastService.getDailyForecast()
            tendency = weatherForecastService.getTendency()
            lastReading = weatherForecastService.getLastReading()
        }

        val commands = listOf(
            DailyWeatherAlertCommand(context, daily),
            StormAlertCommand(context, hourly),
            CurrentWeatherAlertCommand(context, hourly, tendency, lastReading),
        )

        withContext(Dispatchers.Main) {
            commands.forEach { it.execute() }
        }
    }
}