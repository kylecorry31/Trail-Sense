package com.kylecorry.trail_sense.services

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
import com.kylecorry.trail_sense.shared.Constants
import com.kylecorry.trail_sense.shared.PressureAltitudeReading
import com.kylecorry.trail_sense.shared.median
import com.kylecorry.trail_sense.shared.sensors.barometer.Barometer
import com.kylecorry.trail_sense.shared.sensors.gps.GPS
import com.kylecorry.trail_sense.weather.PressureHistoryRepository
import com.kylecorry.trail_sense.weather.forcasting.HourlyForecaster
import com.kylecorry.trail_sense.weather.forcasting.Weather
import com.kylecorry.trail_sense.weather.sealevel.DerivativeSeaLevelPressureConverter
import com.kylecorry.trail_sense.weather.sealevel.NullPressureConverter
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

class BarometerAlarmReceiver: BroadcastReceiver(), Observer {

    private lateinit var context: Context
    private lateinit var barometer: Barometer
    private lateinit var gps: GPS

    private var hasLocation = false
    private var hasBarometerReading = false

    private val altitudeReadings = mutableListOf<Float>()
    private val pressureReadings = mutableListOf<Float>()

    private val forecaster = HourlyForecaster()

    private val MAX_BAROMETER_READINGS = 7
    private val MAX_GPS_READINGS = 5

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null){
            this.context = context
            barometer =
                Barometer(context)
            gps = GPS(context)

            gps.addObserver(this)
            gps.start(Duration.ofSeconds(1))

            barometer.addObserver(this)
            barometer.start()
        }
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == barometer) recordBarometerReading()
        if (o == gps) recordGPSReading()
    }

    private fun recordGPSReading(){
        altitudeReadings.add(gps.altitude.value)

        if (altitudeReadings.size >= MAX_GPS_READINGS) {
            hasLocation = true
            gps.stop()
            gps.deleteObserver(this)

            if (hasBarometerReading) {
                gotAllReadings()
            }
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

    private fun recordBarometerReading(){
        pressureReadings.add(barometer.pressure.value)

        if (pressureReadings.size >= MAX_BAROMETER_READINGS) {
            hasBarometerReading = true
            barometer.stop()
            barometer.deleteObserver(this)

            if (hasLocation) {
                gotAllReadings()
            }
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
        val useSeaLevel = prefs.getBoolean(context.getString(R.string.pref_use_sea_level_pressure), false)

        val pressureConverter = if (useSeaLevel){
            DerivativeSeaLevelPressureConverter(
                Constants.MAXIMUM_NATURAL_PRESSURE_CHANGE
            )
        } else {
            NullPressureConverter()
        }

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