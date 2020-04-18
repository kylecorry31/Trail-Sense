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
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.median
import com.kylecorry.trail_sense.shared.sensors.Barometer
import com.kylecorry.trail_sense.shared.sensors.GPS
import com.kylecorry.trail_sense.shared.sensors.IAltimeter
import com.kylecorry.trail_sense.shared.sensors.IBarometer
import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import com.kylecorry.trail_sense.weather.domain.forcasting.HourlyForecaster
import com.kylecorry.trail_sense.weather.domain.forcasting.Weather
import com.kylecorry.trail_sense.weather.domain.sealevel.SeaLevelPressureConverterFactory
import java.time.Instant
import java.time.ZonedDateTime

class BarometerAlarmReceiver: BroadcastReceiver() {

    private lateinit var context: Context
    private lateinit var barometer: IBarometer
    private lateinit var altimeter: IAltimeter

    private var hasLocation = false
    private var hasBarometerReading = false

    private val altitudeReadings = mutableListOf<Float>()
    private val pressureReadings = mutableListOf<Float>()

    private val forecaster = HourlyForecaster()

    private val MAX_BAROMETER_READINGS = 8
    private val MAX_GPS_READINGS = 5

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null){
            this.context = context
            barometer = Barometer(context)
            altimeter = GPS(context)

            altimeter.start(this::onLocationUpdate)
            barometer.start(this::onPressureUpdate)
        }
    }

    private fun onLocationUpdate(): Boolean {
        altitudeReadings.add(altimeter.altitude)
        return if(altitudeReadings.size >= MAX_GPS_READINGS){
            hasLocation = true
            if (hasBarometerReading){
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
        return if (reading != 0f && lastReading != 0f){
            (1 - alpha) * lastReading + alpha * reading
        } else if (reading != 0f){
            reading
        } else {
            lastReading
        }
    }

    private fun getTruePressure(readings: List<Float>): Float {
        val reading = readings.median()
        val lastReading = getLastPressure()

        val alpha = 0.8f

        return if (reading != 0f && lastReading != 0f){
            (1 - alpha) * lastReading + alpha * reading
        } else if (reading != 0f){
            reading
        } else {
            lastReading
        }
    }

    private fun gotAllReadings(){
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

        if (forecaster.forecast(pressureConverter.convert(readings)) == Weather.Storm){

            val shouldSend = prefs.getBoolean(context.getString(R.string.pref_send_storm_alert), true)
            if (shouldSend && !sentAlert) {
                val builder = NotificationCompat.Builder(context, "Alerts")
                    .setSmallIcon(R.drawable.ic_alert)
                    .setContentTitle("Storm Alert")
                    .setContentText("A storm might be approaching")
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
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}