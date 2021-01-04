package com.kylecorry.trail_sense.astronomy.infrastructure.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.toDisplayFormat
import com.kylecorry.trailsensecore.domain.astronomy.SunTimesMode
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.system.AlarmUtils
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

class SunsetAlarmReceiver : BroadcastReceiver() {

    private lateinit var context: Context
    private lateinit var gps: IGPS
    private lateinit var sensorService: SensorService
    private val gpsTimeout = Intervalometer {
        if (!hasLocation) {
            hasLocation = true
            gotReading()
        }
    }
    private val astronomyService = AstronomyService()

    private var hasLocation = false

    private lateinit var userPrefs: UserPreferences

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "Broadcast received at ${ZonedDateTime.now()}")

        if (context == null) {
            return
        }

        this.context = context.applicationContext
        userPrefs = UserPreferences(this.context)
        val shouldSend = userPrefs.astronomy.sendSunsetAlerts
        if (!shouldSend) {
            return
        }

        sensorService = SensorService(this.context)
        gps = sensorService.getGPS()

        gpsTimeout.once(Duration.ofSeconds(10))

        if (gps.hasValidReading){
            onLocationUpdate()
        } else {
            gps.start(this::onLocationUpdate)
        }
    }

    private fun onLocationUpdate(): Boolean {
        hasLocation = true
        gotReading()
        return false
    }

    private fun gotReading() {
        gps.stop(this::onLocationUpdate)
        gpsTimeout.stop()

        val alertMinutes = userPrefs.astronomy.sunsetAlertMinutesBefore
        val nextAlertMinutes = alertMinutes - 1

        val now = LocalDateTime.now()

        val todaySunset =
            astronomyService.getTodaySunTimes(gps.location, SunTimesMode.Actual).set?.toLocalDateTime()
        val tomorrowSunset =
            astronomyService.getTomorrowSunTimes(gps.location, SunTimesMode.Actual).set?.toLocalDateTime()


        if (todaySunset != null) {
            when {
                withinAlertWindow(todaySunset, alertMinutes) -> {
                    // Send alert, schedule alarm for tomorrow's sunset or else at some point tomorrow
                    sendNotification(todaySunset)
                    setAlarm(tomorrowSunset?.minusMinutes(nextAlertMinutes) ?: todaySunset.plusDays(1))
                }
                isPastSunset(todaySunset) -> {
                    // Missed the sunset, schedule the alarm for tomorrow or else at some point tomorrow
                    setAlarm(tomorrowSunset?.minusMinutes(nextAlertMinutes) ?: todaySunset.plusDays(1))
                }
                else -> { // Before the alert window
                    // Schedule alarm for sunset
                    setAlarm(todaySunset.minusMinutes(nextAlertMinutes))
                }
            }
        } else {
            // There isn't a sunset today, schedule it for tomorrow's sunset or else the same time tomorrow
            setAlarm(tomorrowSunset?.minusMinutes(nextAlertMinutes) ?: now.plusDays(1))
        }

        Log.i(TAG, "Completed at ${ZonedDateTime.now()}")
    }

    private fun isPastSunset(sunset: LocalDateTime): Boolean {
        return LocalDateTime.now().isAfter(sunset)
    }

    private fun withinAlertWindow(sunset: LocalDateTime, alertMinutes: Long): Boolean {
        if (isPastSunset(sunset)){
            return false
        }
        val timeUntilSunset = Duration.between(LocalDateTime.now(), sunset)
        return timeUntilSunset <= Duration.ofMinutes(alertMinutes)
    }

    private fun sendNotification(sunset: LocalDateTime) {

        val lastSentDate = userPrefs.astronomy.sunsetAlertLastSent
        if (LocalDate.now() == lastSentDate) {
            return
        }

        userPrefs.astronomy.setSunsetAlertLastSentDate(LocalDate.now())

        createNotificationChannel()

        val formattedTime = sunset.toDisplayFormat(context)

        val openIntent = MainActivity.astronomyIntent(context)

        val openPendingIntent: PendingIntent =
            PendingIntent.getActivity(context, NOTIFICATION_ID, openIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.sunset)
            .setContentTitle(context.getString(R.string.sunset_alert_notification_title))
            .setContentText(
                context.getString(
                    R.string.sunset_alert_notification_text,
                    formattedTime
                )
            )
            .setAutoCancel(false)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.sunset_alert_channel_title)
            val descriptionText = context.getString(R.string.sunset_alert_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager = context.getSystemService<NotificationManager>()
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun setAlarm(time: LocalDateTime) {
        val lastPi = pendingIntent(context)
        AlarmUtils.cancel(context, lastPi)

        val newPi = pendingIntent(context)
        AlarmUtils.set(context, time, newPi, exact = true, allowWhileIdle = true)
        Log.i(TAG, "Set next sunset alarm at $time")
    }

    companion object {

        const val TAG = "SunsetAlarmReceiver"
        const val NOTIFICATION_ID = 1231
        private const val PI_ID = 8309
        const val NOTIFICATION_CHANNEL_ID = "Sunset alert"

        fun intent(context: Context): Intent {
            return Intent(context, SunsetAlarmReceiver::class.java)
        }

        private fun alarmIntent(context: Context): Intent {
            val i = Intent("com.kylecorry.trail_sense.ALARM_SUNSET")
            i.`package` = PackageUtils.getPackageName(context)
            i.addCategory("android.intent.category.DEFAULT")
            return i
        }

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context, PI_ID, alarmIntent(context), PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}