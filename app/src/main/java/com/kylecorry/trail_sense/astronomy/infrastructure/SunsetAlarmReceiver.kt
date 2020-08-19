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
import kotlin.math.roundToLong

class SunsetAlarmReceiver : BroadcastReceiver() {

    private lateinit var context: Context
    private lateinit var gps: IGPS
    private lateinit var timer: Timer
    private val astronomyService = AstronomyService()

    private var hasLocation = false

    private lateinit var userPrefs: UserPreferences

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("SunsetAlarmReceiver", "Broadcast received")
        if (context != null) {
            this.context = context
            userPrefs = UserPreferences(context)
            val shouldSend = userPrefs.astronomy.sendSunsetAlerts
            if (!shouldSend) {
                return
            }

            gps = GPS(context)

            val that = this
            timer = timer(period = 10000L) {
                if (!hasLocation) {
                    gps.stop(that::onLocationUpdate)
                    hasLocation = true
                }
                cancel()
            }

            if (userPrefs.useLocationFeatures) {
                gps.start(this::onLocationUpdate)
            } else {
                hasLocation = true
            }
        }
    }

    private fun onLocationUpdate(): Boolean {
        return if (hasLocation) {
            hasLocation = true
            gotAllReadings()
            false
        } else {
            true
        }
    }

    private fun gotAllReadings() {
        timer.cancel()

        val alertMinutes = userPrefs.astronomy.sunsetAlertMinutesBefore
        val now = LocalDateTime.now()
        val todaySunset =
            astronomyService.getSunTimes(gps.location, SunTimesMode.Actual, now.toLocalDate()).down
        val tomorrowSunset = astronomyService.getSunTimes(
            gps.location,
            SunTimesMode.Actual,
            now.toLocalDate().plusDays(1)
        ).down

        val nextAlertMinutesBefore = alertMinutes - 1

        if (todaySunset != null) {
            val timeUntilSunset = Duration.between(now, todaySunset)
            if (!timeUntilSunset.isNegative && timeUntilSunset <= Duration.ofMinutes(alertMinutes)) {
                // Send alert, schedule alarm for tomorrow's sunset or else at some point tomorrow
                sendNotification(todaySunset)
                setAlarm(tomorrowSunset?.minusMinutes(nextAlertMinutesBefore) ?: now.plusDays(1))
            } else {
                // Schedule alarm for sunset
                setAlarm(todaySunset.minusMinutes(nextAlertMinutesBefore))
            }
        } else {
            setAlarm(tomorrowSunset?.minusMinutes(nextAlertMinutesBefore) ?: now.plusDays(1))
        }

        Log.i("SunsetAlarmReceiver", "Fired at ${ZonedDateTime.now()}")
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Sunset alert"
            val descriptionText = "Sunset alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("Sunset alert", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager = context.getSystemService<NotificationManager>()
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(sunset: LocalDateTime) {

        val lastSentDate = userPrefs.astronomy.sunsetAlertLastSent
        if (LocalDate.now() == lastSentDate){
            return
        }

        userPrefs.astronomy.setSunsetAlertLastSentDate(LocalDate.now())

        createNotificationChannel()

        val formattedTime = sunset.toDisplayFormat(context)

        val openIntent = Intent(context, MainActivity::class.java)
        openIntent.putExtra(context.getString(R.string.extra_action), R.id.action_astronomy)

        val openPendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context, "Sunset alert")
            .setSmallIcon(R.drawable.moon_waning_crescent)
            .setContentTitle("Sun is setting soon")
            .setContentText("The sun will set at $formattedTime")
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            notify(1231, builder.build())
        }
    }

    private fun setAlarm(time: LocalDateTime) {
        val pi = pendingIntent(context)
        AndroidUtils.cancelAlarm(context, pi)
        AndroidUtils.alarm(context, time, pi)
        Log.i("SunsetAlarmReceiver", "Set next alarm at $time")
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, SunsetAlarmReceiver::class.java)
        }

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context, 1, intent(context), 0
            )
        }
    }
}