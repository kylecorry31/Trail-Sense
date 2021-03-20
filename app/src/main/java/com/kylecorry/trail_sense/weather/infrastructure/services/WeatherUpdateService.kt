package com.kylecorry.trail_sense.weather.infrastructure.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.weather.domain.CanSendDailyForecast
import com.kylecorry.trail_sense.weather.domain.PressureReadingEntity
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.infrastructure.PressureCalibrationUtils
import com.kylecorry.trail_sense.weather.infrastructure.WeatherNotificationService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateScheduler
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import com.kylecorry.trailsensecore.domain.weather.PressureReading
import com.kylecorry.trailsensecore.domain.weather.Weather
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter
import com.kylecorry.trailsensecore.infrastructure.sensors.barometer.IBarometer
import com.kylecorry.trailsensecore.infrastructure.sensors.temperature.IThermometer
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.PowerUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.*

class WeatherUpdateService : Service() {

    private lateinit var barometer: IBarometer
    private lateinit var altimeter: IAltimeter
    private lateinit var thermometer: IThermometer
    private lateinit var sensorService: SensorService
    private val timeout = Intervalometer {
        if (!hasAltitude || !hasTemperatureReading || !hasBarometerReading) {
            hasAltitude = true
            hasTemperatureReading = true
            hasBarometerReading = true
            gotAllReadings()
        }
    }

    private var hasAltitude = false
    private var hasBarometerReading = false
    private var hasTemperatureReading = false
    private var wakelock: PowerManager.WakeLock? = null

    private lateinit var userPrefs: UserPreferences
    private lateinit var weatherService: WeatherService
    private lateinit var pressureRepo: PressureRepo

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notification(
            getString(R.string.weather_update_notification_channel),
            getString(R.string.notification_monitoring_weather),
            R.drawable.ic_update
        )

        startForeground(FOREGROUND_SERVICE_ID, notification)
        Log.i(TAG, "Started at ${ZonedDateTime.now()}")
        acquireWakelock()
        userPrefs = UserPreferences(applicationContext)
        pressureRepo = PressureRepo.getInstance(applicationContext)
        weatherService = WeatherService(
            userPrefs.weather.stormAlertThreshold,
            userPrefs.weather.dailyForecastChangeThreshold,
            userPrefs.weather.hourlyForecastChangeThreshold,
            userPrefs.weather.seaLevelFactorInRapidChanges,
            userPrefs.weather.seaLevelFactorInTemp
        )

        sensorService = SensorService(applicationContext)
        barometer = sensorService.getBarometer()
        altimeter = sensorService.getAltimeter(true)
        thermometer = sensorService.getThermometer()

        scheduleNextUpdate()

        sendWeatherNotification()
        setSensorTimeout(30 * 1000L)
        startSensors()

        return START_NOT_STICKY
    }


    private fun scheduleNextUpdate() {
        val scheduler = WeatherUpdateScheduler.getScheduler(applicationContext)
        scheduler.cancel()
        scheduler.schedule(userPrefs.weather.weatherUpdateFrequency)
    }

    private fun releaseWakelock() {
        try {
            if (wakelock?.isHeld == true) {
                wakelock?.release()
            }
        } catch (e: Exception) {
            // DO NOTHING
        }
    }

    private fun acquireWakelock() {
        try {
            wakelock = PowerUtils.getWakelock(applicationContext, TAG)
            releaseWakelock()
            wakelock?.acquire(60 * 1000L)
        } catch (e: Exception) {
            // DO NOTHING
        }
    }

    private fun sendDailyWeatherNotification(readings: List<PressureReading>) {
        val lastSentDate = userPrefs.weather.dailyWeatherLastSent
        if (LocalDate.now() == lastSentDate) {
            return
        }

        if (!CanSendDailyForecast(userPrefs.weather.dailyForecastTime).isSatisfiedBy(LocalTime.now())){
            return
        }

        userPrefs.weather.dailyWeatherLastSent = LocalDate.now()
        val forecast = weatherService.getDailyWeather(readings)
        val icon = when (forecast) {
            Weather.ImprovingSlow -> R.drawable.sunny
            Weather.WorseningSlow -> R.drawable.light_rain
            else -> R.drawable.steady
        }

        val description = when (forecast) {
            Weather.ImprovingSlow -> getString(if (userPrefs.weather.dailyWeatherIsForTomorrow) R.string.weather_better_than_today  else R.string.weather_better_than_yesterday)
            Weather.WorseningSlow -> getString(if (userPrefs.weather.dailyWeatherIsForTomorrow) R.string.weather_worse_than_today  else R.string.weather_worse_than_yesterday)
            else -> getString(if (userPrefs.weather.dailyWeatherIsForTomorrow) R.string.weather_same_as_today  else R.string.weather_same_as_yesterday)
        }

        val openIntent = MainActivity.weatherIntent(this)
        val openPendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val builder = NotificationUtils.builder(this, DAILY_CHANNEL_ID)
            .setContentTitle(getString(if (userPrefs.weather.dailyWeatherIsForTomorrow) R.string.tomorrows_forecast else R.string.todays_forecast))
            .setContentText(description)
            .setSmallIcon(icon)
            .setLargeIcon(Icon.createWithResource(this, icon))
            .setAutoCancel(false)
            .setContentIntent(openPendingIntent)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            builder.setPriority(Notification.PRIORITY_LOW)
        }

        val notification = builder.build()

        NotificationUtils.send(this, DAILY_NOTIFICATION_ID, notification)
    }

    private fun sendWeatherNotification() {
        runBlocking {
            withContext(Dispatchers.IO) {
                val rawReadings =
                    pressureRepo.getPressuresSync().map { it.toPressureAltitudeReading() }
                        .sortedBy { it.time }

                val readings =
                    PressureCalibrationUtils.calibratePressures(applicationContext, rawReadings)

                withContext(Dispatchers.Main) {
                    val forecast = weatherService.getHourlyWeather(readings)

                    if (userPrefs.weather.shouldShowDailyWeatherNotification) {
                        sendDailyWeatherNotification(readings)
                    }

                    if (userPrefs.weather.shouldShowWeatherNotification) {
                        WeatherNotificationService.updateNotificationForecast(
                            applicationContext,
                            forecast,
                            readings
                        )
                    }
                }
            }
        }

    }

    private fun setSensorTimeout(millis: Long) {
        timeout.once(millis)
    }

    private fun startSensors() {
        if (altimeter.hasValidReading) {
            onAltitudeUpdate()
        } else {
            altimeter.start(this::onAltitudeUpdate)
        }
        barometer.start(this::onPressureUpdate)
        thermometer.start(this::onTemperatureUpdate)
    }

    private fun onAltitudeUpdate(): Boolean {
        hasAltitude = true
        gotAllReadings()
        return false
    }

    private fun onPressureUpdate(): Boolean {
        if (barometer.pressure == 0f) {
            return true
        }
        hasBarometerReading = true
        gotAllReadings()
        return false
    }

    private fun onTemperatureUpdate(): Boolean {
        hasTemperatureReading = true
        gotAllReadings()
        return false
    }

    private fun gotAllReadings() {
        if (!hasAltitude || !hasBarometerReading || !hasTemperatureReading) {
            return
        }
        stopSensors()
        stopTimeout()
        addNewPressureReading()
        sendStormAlert()
        sendWeatherNotification()
        Log.i(TAG, "Got all readings recorded at ${ZonedDateTime.now()}")
        releaseWakelock()
        stopForeground(true)
        stopSelf()
    }

    private fun stopSensors() {
        altimeter.stop(this::onAltitudeUpdate)
        barometer.stop(this::onPressureUpdate)
        thermometer.stop(this::onTemperatureUpdate)
    }

    private fun stopTimeout() {
        timeout.stop()
    }

    private fun addNewPressureReading() {
        if (barometer.pressure == 0f) {
            return
        }
        runBlocking {
            withContext(Dispatchers.IO) {
                pressureRepo.addPressure(
                    PressureReadingEntity(
                        barometer.pressure,
                        altimeter.altitude,
                        0f,
                        if (thermometer.temperature.isNaN()) 16f else thermometer.temperature,
                        Instant.now().toEpochMilli()
                    )
                )
                pressureRepo.deleteOlderThan(Instant.now().minus(Duration.ofDays(2)))
            }
        }
    }

    private fun sendStormAlert() {
        runBlocking {
            withContext(Dispatchers.IO) {
                val cache = Cache(applicationContext)
                val sentAlert =
                    cache.getBoolean(applicationContext.getString(R.string.pref_just_sent_alert))
                        ?: false


                val readings =
                    pressureRepo.getPressuresSync().map { it.toPressureAltitudeReading() }
                        .sortedBy { it.time }
                val forecast = weatherService.getHourlyWeather(
                    PressureCalibrationUtils.calibratePressures(applicationContext, readings)
                )

                if (forecast == Weather.Storm) {
                    val shouldSend = userPrefs.weather.sendStormAlerts
                    if (shouldSend && !sentAlert) {
                        val notification =
                            NotificationCompat.Builder(applicationContext, STORM_CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_alert)
                                .setContentTitle(applicationContext.getString(R.string.notification_storm_alert_title))
                                .setContentText(applicationContext.getString(R.string.notification_storm_alert_text))
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .build()

                        NotificationUtils.send(
                            applicationContext,
                            STORM_ALERT_NOTIFICATION_ID,
                            notification
                        )

                        cache.putBoolean(
                            applicationContext.getString(R.string.pref_just_sent_alert),
                            true
                        )
                    }
                } else {
                    NotificationUtils.cancel(applicationContext, STORM_ALERT_NOTIFICATION_ID)
                    cache.putBoolean(
                        applicationContext.getString(R.string.pref_just_sent_alert),
                        false
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        releaseWakelock()
        stopTimeout()
        stopSensors()
        stopForeground(true)
        stopSelf()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun notification(title: String, content: String, @DrawableRes icon: Int): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(applicationContext, FOREGROUND_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(icon)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setOngoing(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(applicationContext)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(icon)
                .setPriority(Notification.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setOngoing(false)
                .build()
        }
    }

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