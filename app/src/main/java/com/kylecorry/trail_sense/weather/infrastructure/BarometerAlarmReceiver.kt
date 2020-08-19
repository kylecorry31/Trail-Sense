package com.kylecorry.trail_sense.weather.infrastructure

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.median
import com.kylecorry.trail_sense.shared.sensors.*
import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import com.kylecorry.trail_sense.weather.domain.forcasting.HourlyForecaster
import com.kylecorry.trail_sense.weather.domain.forcasting.IWeatherForecaster
import com.kylecorry.trail_sense.weather.domain.forcasting.Weather
import com.kylecorry.trail_sense.weather.domain.sealevel.SeaLevelPressureConverterFactory
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*
import kotlin.concurrent.timer

class BarometerAlarmReceiver : BroadcastReceiver() {

    private lateinit var context: Context
    private lateinit var barometer: IBarometer
    private lateinit var altimeter: IAltimeter
    private lateinit var timer: Timer

    private var hasLocation = false
    private var hasBarometerReading = false

    private val altitudeReadings = mutableListOf<Float>()
    private val pressureReadings = mutableListOf<Float>()

    private lateinit var userPrefs: UserPreferences
    private lateinit var forecaster: IWeatherForecaster

    private val MAX_BAROMETER_READINGS = 8
    private val MAX_GPS_READINGS = 5

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            this.context = context
            userPrefs = UserPreferences(context)
            forecaster = HourlyForecaster(
                userPrefs.weather.stormAlertThreshold,
                userPrefs.weather.hourlyForecastFastThreshold
            )

            barometer = Barometer(context)
            altimeter = GPS(context)

            val that = this
            timer = timer(period = (5000 * (MAX_GPS_READINGS + 2)).toLong()) {
                if (!hasLocation) {
                    altimeter.stop(that::onLocationUpdate)
                    altitudeReadings.add(altimeter.altitude)
                    hasLocation = true
                    if (hasBarometerReading){
                        gotAllReadings()
                    }
                }
                cancel()
            }

            if (userPrefs.useLocationFeatures) {
                altimeter.start(this::onLocationUpdate)
            } else {
                altitudeReadings.add(altimeter.altitude)
                hasLocation = true
            }
            barometer.start(this::onPressureUpdate)
        }
    }

    private fun onLocationUpdate(): Boolean {
        altitudeReadings.add(altimeter.altitude)
        return if (hasLocation || altitudeReadings.size >= MAX_GPS_READINGS) {
            hasLocation = true
            if (hasBarometerReading) {
                gotAllReadings()
            }
            false
        } else {
            true
        }
    }

    private fun onPressureUpdate(): Boolean {
        pressureReadings.add(barometer.pressure)

        return if (pressureReadings.size >= MAX_BAROMETER_READINGS) {
            hasBarometerReading = true
            if (hasLocation) {
                gotAllReadings()
            }
            false
        } else {
            true
        }
    }

    private fun getTrueAltitude(readings: List<Float>): Float {
        val reading = readings.median()
        val lastReading = getLastAltitude()

        val alpha = 0.8f
        return if (reading != 0f && lastReading != 0f) {
            (1 - alpha) * lastReading + alpha * reading
        } else if (reading != 0f) {
            reading
        } else {
            lastReading
        }
    }

    private fun getTruePressure(readings: List<Float>): Float {
        val reading = readings.median()
        val lastReading = getLastPressure()

        val alpha = 0.8f

        return if (reading != 0f && lastReading != 0f) {
            (1 - alpha) * lastReading + alpha * reading
        } else if (reading != 0f) {
            reading
        } else {
            lastReading
        }
    }

    private fun gotAllReadings() {
        altimeter.stop(this::onLocationUpdate)
        barometer.stop(this::onPressureUpdate)
        timer.cancel()
        PressureHistoryRepository.add(
            context,
            PressureAltitudeReading(
                Instant.now(),
                getTruePressure(pressureReadings),
                getTrueAltitude(altitudeReadings)
            )
        )

        createNotificationChannel()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val sentAlert = prefs.getBoolean(context.getString(R.string.pref_just_sent_alert), false)

        val pressureConverter = SeaLevelPressureConverterFactory().create(context)

        val readings = PressureHistoryRepository.getAll(context)

        val forecast = forecaster.forecast(pressureConverter.convert(readings))

        if (forecast == Weather.Storm) {
            val shouldSend = userPrefs.weather.sendStormAlerts
            if (shouldSend && !sentAlert) {
                val builder = NotificationCompat.Builder(context, "Alerts")
                    .setSmallIcon(R.drawable.ic_alert)
                    .setContentTitle(context.getString(R.string.notification_storm_alert_title))
                    .setContentText(context.getString(R.string.notification_storm_alert_text))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)

                with(NotificationManagerCompat.from(context)) {
                    notify(0, builder.build())
                }
                prefs.edit {
                    putBoolean(context.getString(R.string.pref_just_sent_alert), true)
                }
            }
        } else {
            with(NotificationManagerCompat.from(context)) {
                cancel(0)
            }
            prefs.edit {
                putBoolean(context.getString(R.string.pref_just_sent_alert), false)
            }
        }

        WeatherNotificationService.updateNotificationForecast(context, forecast)

        Log.i("BarometerAlarmReceiver", "Got all readings recorded at ${ZonedDateTime.now()}")

    }

    private fun getLastAltitude(): Float {
        PressureHistoryRepository.getAll(
            context
        ).reversed().forEach {
            if (it.altitude != 0F) return it.altitude
        }

        return 0F
    }

    private fun getLastPressure(): Float {
        PressureHistoryRepository.getAll(
            context
        ).reversed().forEach {
            if (it.pressure != 0.0f) return it.pressure
        }

        return 0.0f
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alerts"
            val descriptionText = "Storm alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("Alerts", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager = context.getSystemService<NotificationManager>()
            notificationManager?.createNotificationChannel(channel)
        }
    }
}