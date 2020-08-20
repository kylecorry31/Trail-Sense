package com.kylecorry.trail_sense.astronomy.infrastructure

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
import com.kylecorry.trail_sense.astronomy.domain.sun.SunTimesMode
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.*
import java.time.*
import java.util.*
import kotlin.concurrent.timer

class SunsetAlarmReceiver : BroadcastReceiver() {

    private lateinit var context: Context
    private lateinit var gps: IGPS
    private lateinit var gpsTimeout: Timer
    private val astronomyService = AstronomyService()

    private var hasLocation = false

    private lateinit var userPrefs: UserPreferences

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "Broadcast received at ${ZonedDateTime.now()}")

        if (context == null) {
            return
        }

        this.context = context
        userPrefs = UserPreferences(context)
        val shouldSend = userPrefs.astronomy.sendSunsetAlerts
        if (!shouldSend) {
            return
        }

        gps = GPS(context)

        val that = this
        gpsTimeout = timer(period = 10000L) {
            if (!hasLocation) {
                gps.stop(that::onLocationUpdate)
                hasLocation = true
                gotReading()
            }
            cancel()
        }

        if (userPrefs.useLocationFeatures) {
            gps.start(this::onLocationUpdate)
        } else {
            hasLocation = true
            gotReading()
        }
    }

    private fun onLocationUpdate(): Boolean {
        return if (hasLocation) {
            hasLocation = true
            gotReading()
            false
        } else {
            true
        }
    }

    private fun gotReading() {
        gpsTimeout.cancel()

        val alertMinutes = userPrefs.astronomy.sunsetAlertMinutesBefore
        val nextAlertMinutes = alertMinutes - 1

        val now = LocalDateTime.now()

        val todaySunset =
            astronomyService.getTodaySunTimes(gps.location, SunTimesMode.Actual).down
        val tomorrowSunset =
            astronomyService.getTomorrowSunTimes(gps.location, SunTimesMode.Actual).down


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
        val pi = pendingIntent(context)
        SystemUtils.cancelAlarm(context, pi)
        SystemUtils.alarm(context, time, pi)
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

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context, PI_ID, intent(context), 0
            )
        }
    }
}