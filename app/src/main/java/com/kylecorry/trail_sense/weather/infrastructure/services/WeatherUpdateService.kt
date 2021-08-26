package com.kylecorry.trail_sense.weather.infrastructure.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kylecorry.andromeda.core.sensors.read
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.services.CoroutineForegroundService
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.weather.domain.PressureReadingEntity
import com.kylecorry.trail_sense.weather.infrastructure.WeatherContextualService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.weather.infrastructure.commands.CurrentWeatherAlertCommand
import com.kylecorry.trail_sense.weather.infrastructure.commands.DailyWeatherAlertCommand
import com.kylecorry.trail_sense.weather.infrastructure.commands.StormAlertCommand
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import com.kylecorry.trailsensecore.domain.weather.PressureReading
import com.kylecorry.trailsensecore.domain.weather.PressureTendency
import com.kylecorry.trailsensecore.domain.weather.Weather
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

class WeatherUpdateService : CoroutineForegroundService() {

    private val sensorService by lazy { SensorService(this) }
    private val altimeter by lazy { sensorService.getGPSAltimeter(true) }
    private val barometer by lazy { sensorService.getBarometer() }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val hygrometer by lazy { sensorService.getHygrometer() }
    private val prefs by lazy { UserPreferences(this) }
    private val pressureRepo by lazy { PressureRepo.getInstance(this) }
    private val weatherForecastService by lazy { WeatherContextualService.getInstance(this) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override suspend fun doWork() {
        Log.i(TAG, "Started at ${ZonedDateTime.now()}")
        withContext(Dispatchers.Main) {
            acquireWakelock(TAG, Duration.ofMinutes(1))
            scheduleNextUpdate()
        }

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

        withContext(Dispatchers.Main) {
            stopService(true)
        }
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
        Log.i(TAG, "Got all readings recorded at ${ZonedDateTime.now()}")
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

        withContext(Dispatchers.Main) {
            DailyWeatherAlertCommand(this@WeatherUpdateService, daily).execute()
            StormAlertCommand(this@WeatherUpdateService, hourly).execute()
            CurrentWeatherAlertCommand(this@WeatherUpdateService, hourly, tendency, lastReading).execute()
        }
    }

    private fun scheduleNextUpdate() {
        val scheduler = WeatherUpdateScheduler.getScheduler(applicationContext)
        scheduler.cancel()
        scheduler.schedule(prefs.weather.weatherUpdateFrequency)
    }

    override fun getForegroundNotification(): Notification {
        return Notify.background(
            this,
            NotificationChannels.CHANNEL_BACKGROUND_UPDATES,
            getString(R.string.weather_update_notification_channel),
            getString(R.string.notification_monitoring_weather),
            R.drawable.ic_update
        )
    }

    override val foregroundNotificationId: Int = FOREGROUND_SERVICE_ID

    companion object {
        const val DAILY_CHANNEL_ID = "daily-weather"
        private const val FOREGROUND_SERVICE_ID = 629579783
        const val STORM_CHANNEL_ID = "Alerts"
        const val WEATHER_CHANNEL_ID = "Weather"
        private const val TAG = "WeatherUpdateService"

        fun intent(context: Context): Intent {
            return Intent(context, WeatherUpdateService::class.java)
        }

        fun start(context: Context) {
            Intents.startService(context, intent(context), foreground = true)
        }
    }
}