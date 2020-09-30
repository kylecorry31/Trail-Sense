package com.kylecorry.trail_sense.weather.infrastructure.receivers

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.system.AlarmUtils
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherNotificationService
import com.kylecorry.trail_sense.weather.infrastructure.database.PressureRepo
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.domain.weather.Weather
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter
import com.kylecorry.trailsensecore.infrastructure.sensors.barometer.IBarometer
import com.kylecorry.trailsensecore.infrastructure.sensors.temperature.IThermometer
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
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

    private lateinit var userPrefs: UserPreferences
    private lateinit var weatherService: WeatherService
    private lateinit var pressureRepo: PressureRepo

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Started at ${ZonedDateTime.now()}")
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

        sendWeatherNotification()
        setSensorTimeout(30000L)
        startSensors()

        return START_STICKY_COMPATIBILITY
    }

    private fun sendWeatherNotification() {
        val readings = weatherService.convertToSeaLevel(pressureRepo.get().toList())
        val forecast = weatherService.getHourlyWeather(readings)

        if (userPrefs.weather.shouldShowWeatherNotification || userPrefs.weather.foregroundService) {
            WeatherNotificationService.updateNotificationForecast(applicationContext, forecast, readings)
        }
    }

    private fun canRun(): Boolean {
        val threshold = Duration.ofMinutes(5)
        val lastCalled = Duration.between(getLastUpdatedTime(), LocalDateTime.now())

        return lastCalled >= threshold
    }

    private fun getLastUpdatedTime(): LocalDateTime {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val raw = prefs.getString(LAST_CALLED_KEY, LocalDateTime.MIN.toString())
            ?: LocalDateTime.MIN.toString()
        return LocalDateTime.parse(raw)
    }

    private fun setLastUpdatedTime() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        prefs.edit {
            putString(LAST_CALLED_KEY, LocalDateTime.now().toString())
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
        if (barometer.pressure == 0f){
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
        val sentAlert = prefs.getBoolean(applicationContext.getString(R.string.pref_just_sent_alert), false)

        val readings = pressureRepo.get().toList()
        val forecast = weatherService.getHourlyWeather(weatherService.convertToSeaLevel(readings))

        if (forecast == Weather.Storm) {
            val shouldSend = userPrefs.weather.sendStormAlerts
            if (shouldSend && !sentAlert) {
                val notification = NotificationCompat.Builder(applicationContext, STORM_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_alert)
                    .setContentTitle(applicationContext.getString(R.string.notification_storm_alert_title))
                    .setContentText(applicationContext.getString(R.string.notification_storm_alert_text))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

                NotificationUtils.send(applicationContext, STORM_ALERT_NOTIFICATION_ID, notification)

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

    private fun createNotificationChannel() {
        NotificationUtils.createChannel(
            applicationContext, STORM_CHANNEL_ID,
            applicationContext.getString(R.string.notification_storm_alert_channel_name),
            applicationContext.getString(R.string.notification_storm_alert_channel_desc),
            NotificationUtils.CHANNEL_IMPORTANCE_HIGH
        )
    }

    companion object {

        private const val STORM_CHANNEL_ID = "Alerts"
        private const val TAG = "WeatherUpdateService"
        private const val STORM_ALERT_NOTIFICATION_ID = 74309823

        fun intent(context: Context): Intent {
            return Intent(context, WeatherUpdateService::class.java)
        }

        private const val LAST_CALLED_KEY = "weatherLastUpdated"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}