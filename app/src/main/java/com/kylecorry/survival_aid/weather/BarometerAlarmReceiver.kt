package com.kylecorry.survival_aid.weather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kylecorry.survival_aid.R
import java.util.*

class BarometerAlarmReceiver: BroadcastReceiver(), Observer {

    private lateinit var context: Context
    private lateinit var barometer: Barometer
    private var sentAlert = false

    override fun update(o: Observable?, arg: Any?) {
        PressureHistory.addReading(barometer.pressure)

        createNotificationChannel()

        if (WeatherUtils.isStormIncoming(PressureHistory.readings)){
            if (!sentAlert) {
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
        barometer.stop()
        barometer.deleteObserver(this)
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

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null){
            this.context = context
            barometer = Barometer(context)
            barometer.addObserver(this)
            barometer.start()
        }
    }
}