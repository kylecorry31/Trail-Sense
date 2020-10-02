package com.kylecorry.trail_sense.weather.infrastructure.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.PowerUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherNotificationService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateWorker
import com.kylecorry.trail_sense.weather.infrastructure.database.PressureRepo
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.domain.weather.Weather
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter
import com.kylecorry.trailsensecore.infrastructure.sensors.barometer.IBarometer
import com.kylecorry.trailsensecore.infrastructure.sensors.temperature.IThermometer
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

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
        Log.i(TAG, "Started at ${ZonedDateTime.now()}")
        wakelock = PowerUtils.getWakelock(applicationContext, TAG)
        wakelock?.acquire(10 * 60 * 1000L)
        userPrefs = UserPreferences(applicationContext)
        pressureRepo = PressureRepo(applicationContext)
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

        WeatherUpdateWorker.start(applicationContext, userPrefs.weather.weatherUpdateFrequency)


        createChannel(
            applicationContext,
            FOREGROUND_CHANNEL_ID,
            getString(R.string.weather_update_notification_channel),
            getString(R.string.weather_update_notification_channel_desc),
            NotificationUtils.CHANNEL_IMPORTANCE_LOW,
            false
        )
        val notification = notification(
            getString(R.string.weather_update_notification_channel),
            getString(R.string.notification_monitoring_weather),
            R.drawable.ic_update
        )

        startForeground(FOREGROUND_SERVICE_ID, notification)

        sendWeatherNotification()
        setSensorTimeout(30000L)
        startSensors()

        return START_NOT_STICKY
    }

    private fun sendWeatherNotification() {
        val readings = weatherService.convertToSeaLevel(
            pressureRepo.get().toList(),
            userPrefs.weather.requireDwell
        )
        val forecast = weatherService.getHourlyWeather(readings)

        if (userPrefs.weather.shouldShowWeatherNotification) {
            WeatherNotificationService.updateNotificationForecast(
                applicationContext,
                forecast,
                readings
            )
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
        wakelock?.release()
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
        pressureRepo.add(
            PressureAltitudeReading(
                Instant.now(),
                barometer.pressure,
                altimeter.altitude,
                if (thermometer.temperature.isNaN()) 16f else thermometer.temperature
            )
        )
        pressureRepo.deleteOlderThan(Instant.now().minus(Duration.ofHours(48)))
    }

    private fun sendStormAlert() {
        createNotificationChannel()
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val sentAlert =
            prefs.getBoolean(applicationContext.getString(R.string.pref_just_sent_alert), false)

        val readings = pressureRepo.get().toList()
        val forecast = weatherService.getHourlyWeather(
            weatherService.convertToSeaLevel(
                readings,
                userPrefs.weather.requireDwell
            )
        )

        if (forecast == Weather.Storm) {
            val shouldSend = userPrefs.weather.sendStormAlerts
            if (shouldSend && !sentAlert) {
                val notification = NotificationCompat.Builder(applicationContext, STORM_CHANNEL_ID)
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

                prefs.edit {
                    putBoolean(applicationContext.getString(R.string.pref_just_sent_alert), true)
                }
            }
        } else {
            NotificationUtils.cancel(applicationContext, STORM_ALERT_NOTIFICATION_ID)
            prefs.edit {
                putBoolean(applicationContext.getString(R.string.pref_just_sent_alert), false)
            }
        }
    }

    override fun onDestroy() {
        stopForeground(true)
        stopSelf()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        NotificationUtils.createChannel(
            applicationContext, STORM_CHANNEL_ID,
            applicationContext.getString(R.string.notification_storm_alert_channel_name),
            applicationContext.getString(R.string.notification_storm_alert_channel_desc),
            NotificationUtils.CHANNEL_IMPORTANCE_HIGH
        )
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

    fun createChannel(
        context: Context,
        id: String,
        name: String,
        description: String,
        importance: Int,
        sound: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(id, name, importance).apply {
            this.description = description
            if (!sound) {
                setSound(null, null)
            }
        }
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    companion object {

        private const val FOREGROUND_SERVICE_ID = 629579783
        private const val STORM_CHANNEL_ID = "Alerts"
        private const val FOREGROUND_CHANNEL_ID = "WeatherUpdate"
        private const val TAG = "WeatherUpdateService"
        private const val STORM_ALERT_NOTIFICATION_ID = 74309823

        fun intent(context: Context): Intent {
            return Intent(context, WeatherUpdateService::class.java)
        }

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent(context))
            } else {
                context.startService(intent(context))
            }
        }
    }
}