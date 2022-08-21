package com.kylecorry.trail_sense.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.altimeter.MedianAltimeter
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.weather.infrastructure.persistence.WeatherRepo
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant

class MonitorWeatherCommand(private val context: Context, private val background: Boolean = true) :
    CoroutineCommand {

    private val sensorService by lazy { SensorService(context) }
    private val altimeter by lazy { MedianAltimeter(sensorService.getGPSAltimeter(background)) }
    private val barometer by lazy { sensorService.getBarometer() }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val hygrometer by lazy { sensorService.getHygrometer() }
    private val repo by lazy { WeatherRepo.getInstance(context) }
    private val weatherForecastService by lazy { WeatherSubsystem.getInstance(context) }

    override suspend fun execute() {
        sendWeatherNotifications()

        try {
            withTimeoutOrNull(Duration.ofSeconds(10).toMillis()) {
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
        } finally {
            forceStopSensors()
        }

        recordReading()
        sendWeatherNotifications()
    }

    private fun forceStopSensors() {
        // This shouldn't be needed, but for some reason the GPS got stuck on
        altimeter.stop(null)
        barometer.stop(null)
        thermometer.stop(null)
        hygrometer.stop(null)
    }


    private suspend fun recordReading() {
        if (barometer.pressure == 0f) {
            return
        }
        onIO {
            repo.add(
                Reading(
                    RawWeatherObservation(
                        0,
                        barometer.pressure,
                        altimeter.altitude,
                        if (thermometer.temperature.isNaN()) 16f else thermometer.temperature,
                        if (altimeter.altimeter is IGPS) ((altimeter.altimeter as IGPS).verticalAccuracy ?: 0f) else 0f,
                        hygrometer.humidity,
                    ),
                    Instant.now()
                )
            )
        }
    }

    private suspend fun sendWeatherNotifications() {
        val weather = onIO { weatherForecastService.getWeather() }

        val commands = listOfNotNull(
            DailyWeatherAlertCommand(context, weather.prediction.daily),
            StormAlertCommand(context, weather.prediction.hourly),
            weather.observation?.let {
                CurrentWeatherAlertCommand(
                    context,
                    weather.prediction.hourly,
                    weather.pressureTendency,
                    it.pressureReading()
                )
            }
        )

        withContext(Dispatchers.Main) {
            commands.forEach { it.execute() }
        }
    }
}