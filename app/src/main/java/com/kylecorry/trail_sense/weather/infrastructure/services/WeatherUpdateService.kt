package com.kylecorry.trail_sense.weather.infrastructure.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.weather.domain.CanSendDailyForecast
import com.kylecorry.trail_sense.weather.domain.PressureReadingEntity
import com.kylecorry.trail_sense.weather.infrastructure.WeatherContextualService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherNotificationService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import com.kylecorry.trailsensecore.domain.weather.PressureReading
import com.kylecorry.trailsensecore.domain.weather.PressureTendency
import com.kylecorry.trailsensecore.domain.weather.Weather
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.sensors.read
import com.kylecorry.andromeda.services.CoroutineForegroundService
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import kotlinx.coroutines.*
import java.time.*

class WeatherUpdateService: CoroutineForegroundService() {

    private val notify by lazy { Notify(this) }
    private val sensorService by lazy { SensorService(this) }
    private val altimeter by lazy { sensorService.getGPSAltimeter(true) }
    private val barometer by lazy { sensorService.getBarometer() }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val hygrometer by lazy { sensorService.getHygrometer() }
    private val prefs by lazy { UserPreferences(this) }
    private val pressureRepo by lazy { PressureRepo.getInstance(this) }
    private val cache by lazy { Cache(this) }
    private val weatherForecastService by lazy { WeatherContextualService.getInstance(this) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override suspend fun doWork() {
        Log.i(TAG, "Started at ${ZonedDateTime.now()}")
        withContext(Dispatchers.Main){
            acquireWakelock(TAG, Duration.ofMinutes(1))
            scheduleNextUpdate()
        }

        sendWeatherNotifications()

        withTimeoutOrNull(Duration.ofSeconds(30).toMillis()) {
            val jobs = mutableListOf<Job>()
            if (!altimeter.hasValidReading) {
                jobs.add(launch { altimeter.read() })
            }

            if (!barometer.hasValidReading){
                jobs.add(launch { barometer.read() })
            }

            if (!thermometer.hasValidReading){
                jobs.add(launch { thermometer.read() })
            }

            if (!hygrometer.hasValidReading){
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

    private suspend fun recordReading(){
        if (barometer.pressure == 0f){
            return
        }
        withContext(Dispatchers.IO){
            pressureRepo.addPressure(PressureReadingEntity(
                barometer.pressure,
                altimeter.altitude,
                if (altimeter is IGPS) ((altimeter as IGPS).verticalAccuracy ?: 0f) else 0f,
                if (thermometer.temperature.isNaN()) 16f else thermometer.temperature,
                hygrometer.humidity,
                Instant.now().toEpochMilli()
            ))
            pressureRepo.deleteOlderThan(Instant.now().minus(Duration.ofDays(2)))
        }
        Log.i(TAG, "Got all readings recorded at ${ZonedDateTime.now()}")
    }

    private suspend fun sendWeatherNotifications(){

        var hourly: Weather
        var daily: Weather
        var tendency: PressureTendency
        var lastReading: PressureReading? = null
        withContext(Dispatchers.IO){
            hourly = weatherForecastService.getHourlyForecast()
            daily = weatherForecastService.getDailyForecast()
            tendency = weatherForecastService.getTendency()
            lastReading = weatherForecastService.getLastReading()
        }

        withContext(Dispatchers.Main) {

            if (prefs.weather.shouldShowDailyWeatherNotification) {
                sendDailyWeatherNotification(daily)
            }

            handleStormAlert(hourly)

            if (prefs.weather.shouldShowWeatherNotification) {
                WeatherNotificationService.updateNotificationForecast(
                    this@WeatherUpdateService,
                    hourly,
                    tendency,
                    lastReading
                )
            }
        }
    }

    private fun handleStormAlert(forecast: Weather){
        val sentAlert = cache.getBoolean(getString(R.string.pref_just_sent_alert)) ?: false

        if (forecast == Weather.Storm) {
            val shouldSend = prefs.weather.sendStormAlerts
            if (shouldSend && !sentAlert) {
                val notification = notify.alert(
                    STORM_CHANNEL_ID,
                    getString(R.string.notification_storm_alert_title),
                    getString(R.string.notification_storm_alert_text),
                    R.drawable.ic_alert,
                    group = NotificationChannels.GROUP_STORM,
                    intent = NavigationUtils.pendingIntent(this, R.id.action_weather)
                )
                notify.send(STORM_ALERT_NOTIFICATION_ID, notification)
                cache.putBoolean(getString(R.string.pref_just_sent_alert), true)
            }
        } else {
            notify.cancel(STORM_ALERT_NOTIFICATION_ID)
            cache.putBoolean(getString(R.string.pref_just_sent_alert), false)
        }
    }

    private fun sendDailyWeatherNotification(forecast: Weather) {
        val lastSentDate = prefs.weather.dailyWeatherLastSent
        if (LocalDate.now() == lastSentDate) {
            return
        }

        if (!CanSendDailyForecast(prefs.weather.dailyForecastTime).isSatisfiedBy(LocalTime.now())) {
            return
        }

        prefs.weather.dailyWeatherLastSent = LocalDate.now()
        val icon = when (forecast) {
            Weather.ImprovingSlow -> R.drawable.sunny
            Weather.WorseningSlow -> R.drawable.light_rain
            else -> R.drawable.steady
        }

        val description = when (forecast) {
            Weather.ImprovingSlow -> getString(if (prefs.weather.dailyWeatherIsForTomorrow) R.string.weather_better_than_today else R.string.weather_better_than_yesterday)
            Weather.WorseningSlow -> getString(if (prefs.weather.dailyWeatherIsForTomorrow) R.string.weather_worse_than_today else R.string.weather_worse_than_yesterday)
            else -> getString(if (prefs.weather.dailyWeatherIsForTomorrow) R.string.weather_same_as_today else R.string.weather_same_as_yesterday)
        }

        val openIntent = NavigationUtils.pendingIntent(this, R.id.action_weather)

        val notification = notify.status(
            DAILY_CHANNEL_ID,
            getString(if (prefs.weather.dailyWeatherIsForTomorrow) R.string.tomorrows_forecast else R.string.todays_forecast),
            description,
            icon,
            showBigIcon = prefs.weather.showColoredNotificationIcon,
            group = NotificationChannels.GROUP_DAILY_WEATHER,
            intent = openIntent
        )

        notify.send(DAILY_NOTIFICATION_ID, notification)
    }

    private fun scheduleNextUpdate() {
        val scheduler = WeatherUpdateScheduler.getScheduler(applicationContext)
        scheduler.cancel()
        scheduler.schedule(prefs.weather.weatherUpdateFrequency)
    }

    override fun getForegroundNotification(): Notification {
        return notify.background(
            NotificationChannels.CHANNEL_BACKGROUND_UPDATES,
            getString(R.string.weather_update_notification_channel),
            getString(R.string.notification_monitoring_weather),
            R.drawable.ic_update
        )
    }

    override val foregroundNotificationId: Int = FOREGROUND_SERVICE_ID

    companion object {
        const val DAILY_CHANNEL_ID = "daily-weather"
        private const val DAILY_NOTIFICATION_ID = 798643
        private const val FOREGROUND_SERVICE_ID = 629579783
        const val STORM_CHANNEL_ID = "Alerts"
        const val WEATHER_CHANNEL_ID = "Weather"
        private const val TAG = "WeatherUpdateService"
        private const val STORM_ALERT_NOTIFICATION_ID = 74309823

        fun intent(context: Context): Intent {
            return Intent(context, WeatherUpdateService::class.java)
        }

        fun start(context: Context) {
            IntentUtils.startService(context, intent(context), foreground = true)
        }
    }
}