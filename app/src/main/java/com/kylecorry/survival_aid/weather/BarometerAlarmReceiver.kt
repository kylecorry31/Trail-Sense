package com.kylecorry.survival_aid.weather

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
import com.kylecorry.survival_aid.R
import com.kylecorry.survival_aid.navigator.gps.GPS
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.abs

class BarometerAlarmReceiver: BroadcastReceiver(), Observer {

    private lateinit var context: Context
    private lateinit var barometer: Barometer
    private lateinit var gps: GPS

    private var hasLocation = false
    private var hasBarometerReading = false

    private val altitudeReadings = mutableListOf<Float>()
    private val pressureReadings = mutableListOf<Float>()

    private val MAX_BAROMETER_READINGS = 7
    private val MAX_GPS_READINGS = 5

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null){
            this.context = context
            barometer = Barometer(context)
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
        altitudeReadings.add(gps.altitude.toFloat())

        if (altitudeReadings.size >= MAX_GPS_READINGS) {
            hasLocation = true
            gps.stop()
            gps.deleteObserver(this)

            if (hasBarometerReading) {
                gotAllReadings()
            }
        }
    }

    private fun getBestReadings(readings: List<Float>, threshold: Float = 5f): List<Float> {
        var bestReadings = mutableListOf<Float>()
        for (i in readings.indices){
            val same = mutableListOf<Float>()
            for (j in readings.indices){
                val diff = abs(readings[i] - readings[j])
                if (diff <= threshold){
                    same.add(readings[j])
                }
            }

            if (same.size > bestReadings.size){
                bestReadings = same
            }
        }

        return bestReadings
    }

    private fun getTrueAltitude(readings: List<Float>): Float {
        val bestReadings = getBestReadings(readings, 10f)

        val average = bestReadings.average().toFloat()

        if (average != 0f && bestReadings.size > 1){
            return average
        }

        val lastAltitude = getLastAltitude()
        if (lastAltitude == 0.0 && bestReadings.size == 1){
            return bestReadings[0]
        }

        return lastAltitude.toFloat()
    }

    private fun getTruePressure(readings: List<Float>): Float {
        val bestReadings = getBestReadings(readings, 0.1f)

        if (bestReadings.size > 1){
            return bestReadings.average().toFloat()
        }

        val lastPressure = getLastPressure()
        if (lastPressure == 0.0f && bestReadings.size == 1){
            return bestReadings[0]
        }

        return lastPressure
    }

    private fun recordBarometerReading(){
        pressureReadings.add(barometer.pressure)

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
        if (PressureHistory.readings.isEmpty()){
            loadFromFile(context)
        }

        PressureHistory.addReading(getTruePressure(pressureReadings), getTrueAltitude(altitudeReadings).toDouble())
        saveToFile(context)

        createNotificationChannel()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val sentAlert = prefs.getBoolean(context.getString(R.string.pref_just_sent_alert), false)
        val useSeaLevel = prefs.getBoolean(context.getString(R.string.pref_use_sea_level_pressure), false)

        if (WeatherUtils.isStormIncoming(PressureHistory.readings, useSeaLevel)){

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

    private fun getLastAltitude(): Double {
        if (PressureHistory.readings.isEmpty()){
            loadFromFile(context)
        }

        PressureHistory.readings.reversed().forEach {
            if (it.altitude != 0.0) return it.altitude
        }

        return 0.0
    }

    private fun getLastPressure(): Float {
        if (PressureHistory.readings.isEmpty()){
            loadFromFile(context)
        }

        PressureHistory.readings.reversed().forEach {
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


    companion object {
        private const val FILE_NAME = "pressure.csv"

        fun loadFromFile(context: Context) {
            val readings = mutableListOf<PressureReading>()
            if (!context.getFileStreamPath(FILE_NAME).exists()) return
            context.openFileInput(FILE_NAME).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val splitLine = line.split(",")
                    val time = splitLine[0].toLong()
                    val pressure = splitLine[1].toFloat()
                    val altitude = splitLine[2].toDouble()
                    readings.add(PressureReading(Instant.ofEpochMilli(time), pressure, altitude))
                }
            }
            PressureHistory.setReadings(readings)
        }


        fun saveToFile(context: Context){
            context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
                val output = PressureHistory.readings.joinToString("\n") { reading ->
                    "${reading.time.toEpochMilli()},${reading.pressure},${reading.altitude}"
                }
                it.write(output.toByteArray())
            }
        }
    }
}