package com.kylecorry.survival_aid.weather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.kylecorry.survival_aid.R
import com.kylecorry.survival_aid.navigator.gps.GPS
import java.time.Instant
import java.util.*

class BarometerAlarmReceiver: BroadcastReceiver(), Observer {

    private lateinit var context: Context
    private lateinit var barometer: Barometer
    private lateinit var gps: GPS
    private var sentAlert = false

    private var hasLocation = false
    private var hasBarometerReading = false

    private var altitude = 0.0
    private var reading: Float = 0f

    private var numBarometerReadings = 0
    private val MAX_BAROMETER_READINGS = 5

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null){
            this.context = context
            barometer = Barometer(context)
            gps = GPS(context)

            gps.updateLocation {
                gps.updateLocation {
                    altitude = if (it == null || gps.altitude == 0.0){
                        getLastAltitude()
                    } else {
                        gps.altitude
                    }
                    hasLocation = true
                    if (hasBarometerReading){
                        gotAllReadings()
                    }
                }
            }

            barometer.addObserver(this)
            barometer.start()
        }
    }

    override fun update(o: Observable?, arg: Any?) {
        reading =+ barometer.pressure

        if (numBarometerReadings == MAX_BAROMETER_READINGS) {
            reading /= MAX_BAROMETER_READINGS
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
        PressureHistory.addReading(reading, altitude)
        saveToFile(context)

        createNotificationChannel()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
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
                sentAlert = true
            }
        } else {
            with(NotificationManagerCompat.from(context)) {
                cancel(0)
            }
            sentAlert = false
        }
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
                    val altitude = if (splitLine.size == 3) splitLine[2].toDouble() else 0.0
                    readings.add(PressureReading(Instant.ofEpochMilli(time), pressure, altitude))
                }
            }
            PressureHistory.setReadings(readings)
        }


        fun saveToFile(context: Context){
            context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
                val output = PressureHistory.readings.joinToString("\n") { reading ->
                    "${reading.time.toEpochMilli()},${reading.reading},${reading.altitude}"
                }
                it.write(output.toByteArray())
            }
        }
    }
}