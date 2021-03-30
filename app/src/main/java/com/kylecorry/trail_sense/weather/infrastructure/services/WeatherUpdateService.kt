package com.kylecorry.trail_sense.weather.infrastructure.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomNotificationUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.services.CoroutineForegroundService
import com.kylecorry.trail_sense.weather.domain.CanSendDailyForecast
import com.kylecorry.trail_sense.weather.domain.PressureReadingEntity
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.infrastructure.PressureCalibrationUtils
import com.kylecorry.trail_sense.weather.infrastructure.WeatherNotificationService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import com.kylecorry.trailsensecore.domain.weather.PressureReading
import com.kylecorry.trailsensecore.domain.weather.Weather
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.sensors.read
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import kotlinx.coroutines.*
import java.time.*

class WeatherUpdateService: CoroutineForegroundService() {

    private val sensorService by lazy { SensorService(this) }
    private val altimeter by lazy { sensorService.getAltimeter(true) }
    private val barometer by lazy { sensorService.getBarometer() }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val hygrometer by lazy { sensorService.getHygrometer() }
    private val prefs by lazy { UserPreferences(this) }
    private val pressureRepo by lazy { PressureRepo.getInstance(this) }
    private val cache by lazy { Cache(this) }
    private val weatherService by lazy {
        WeatherService(
            prefs.weather.stormAlertThreshold,
            prefs.weather.dailyForecastChangeThreshold,
            prefs.weather.hourlyForecastChangeThreshold,
            prefs.weather.seaLevelFactorInRapidChanges,
            prefs.weather.seaLevelFactorInTemp
        )
    }

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
        val readings = withContext(Dispatchers.IO){
            pressureRepo.getPressuresSync()
                .map { it.toPressureAltitudeReading() }
                .sortedBy { it.time }
                .filter { it.time <= Instant.now() }
        }

        withContext(Dispatchers.Main) {
            val calibratedReadings = PressureCalibrationUtils.calibratePressures(this@WeatherUpdateService, readings)

            val hourlyForecast = weatherService.getHourlyWeather(calibratedReadings)

            if (prefs.weather.shouldShowDailyWeatherNotification) {
                sendDailyWeatherNotification(calibratedReadings)
            }

            handleStormAlert(hourlyForecast)

            if (prefs.weather.shouldShowWeatherNotification) {
                WeatherNotificationService.updateNotificationForecast(
                    this@WeatherUpdateService,
                    hourlyForecast,
                    calibratedReadings
                )
            }
        }
    }

    private fun handleStormAlert(forecast: Weather){
        val sentAlert = cache.getBoolean(getString(R.string.pref_just_sent_alert)) ?: false

        if (forecast == Weather.Storm) {
            val shouldSend = prefs.weather.sendStormAlerts
            if (shouldSend && !sentAlert) {
                val notification = CustomNotificationUtils.alert(
                    this,
                    STORM_CHANNEL_ID,
                    getString(R.string.notification_storm_alert_title),
                    getString(R.string.notification_storm_alert_text),
                    R.drawable.ic_alert,
                    group = NotificationChannels.GROUP_STORM
                )
                NotificationUtils.send(this, STORM_ALERT_NOTIFICATION_ID, notification)
                cache.putBoolean(getString(R.string.pref_just_sent_alert), true)
            }
        } else {
            NotificationUtils.cancel(this, STORM_ALERT_NOTIFICATION_ID)
            cache.putBoolean(getString(R.string.pref_just_sent_alert), false)
        }
    }

    private fun sendDailyWeatherNotification(readings: List<PressureReading>) {
        val lastSentDate = prefs.weather.dailyWeatherLastSent
        if (LocalDate.now() == lastSentDate) {
            return
        }

        if (!CanSendDailyForecast(prefs.weather.dailyForecastTime).isSatisfiedBy(LocalTime.now())) {
            return
        }

        prefs.weather.dailyWeatherLastSent = LocalDate.now()
        val forecast = weatherService.getDailyWeather(readings)
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

        val openIntent = MainActivity.weatherIntent(this)
        val openPendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val notification = CustomNotificationUtils.status(
            this,
            DAILY_CHANNEL_ID,
            getString(if (prefs.weather.dailyWeatherIsForTomorrow) R.string.tomorrows_forecast else R.string.todays_forecast),
            description,
            icon,
            showBigIcon = true,
            group = NotificationChannels.GROUP_DAILY_WEATHER,
            intent = openPendingIntent
        )

        NotificationUtils.send(this, DAILY_NOTIFICATION_ID, notification)
    }

    private fun scheduleNextUpdate() {
        val scheduler = WeatherUpdateScheduler.getScheduler(applicationContext)
        scheduler.cancel()
        scheduler.schedule(prefs.weather.weatherUpdateFrequency)
    }

    override fun getForegroundNotification(): Notification {
        return CustomNotificationUtils.background(
            this,
            FOREGROUND_CHANNEL_ID,
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
        const val FOREGROUND_CHANNEL_ID = "WeatherUpdate"
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