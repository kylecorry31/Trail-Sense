package com.kylecorry.trail_sense.shared

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherNotificationService
import java.time.Duration
import java.time.LocalDateTime

object SystemUtils {

    /**
     * Create an alarm
     * @param context The context
     * @param time The time to fire the alarm
     * @param pendingIntent The pending intent to launch when the alarm fires
     * @param exact True if the alarm needs to fire at exactly the time specified, false otherwise
     */
    fun alarm(
        context: Context,
        time: LocalDateTime,
        pendingIntent: PendingIntent,
        exact: Boolean = true
    ) {
        val alarmManager = getAlarmManager(context)
        if (exact) {
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, time.toEpochMillis(), pendingIntent)
        } else {
            alarmManager?.set(AlarmManager.RTC_WAKEUP, time.toEpochMillis(), pendingIntent)
        }
    }

    /**
     * Create a repeating alarm
     * @param context The context
     * @param startTime The time to fire the alarm
     * @param interval The interval to fire the alarm at after the first alarm is fired
     * @param pendingIntent The pending intent to launch when the alarm fires
     * @param exact True if the alarm needs to fire at exactly the time specified, false otherwise
     */
    fun repeatingAlarm(
        context: Context,
        startTime: LocalDateTime,
        interval: Duration,
        pendingIntent: PendingIntent,
        exact: Boolean = true
    ) {
        val alarmManager = getAlarmManager(context)
        if (exact) {
            alarmManager?.setRepeating(
                AlarmManager.RTC_WAKEUP,
                startTime.toEpochMillis(),
                interval.toMillis(),
                pendingIntent
            )
        } else {
            alarmManager?.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                startTime.toEpochMillis(),
                interval.toMillis(),
                pendingIntent
            )
        }
    }

    /**
     * Cancel the alarm associated with the pending intent
     * @param context The context
     * @param pendingIntent The pending intent to cancel
     */
    fun cancelAlarm(context: Context, pendingIntent: PendingIntent) {
        try {
            val alarmManager = getAlarmManager(context)
            alarmManager?.cancel(pendingIntent)
        } catch (e: Exception) {
            Log.e("SystemUtils", "Could not cancel alarm", e)
        }
    }

    fun sendNotification(context: Context, notificationId: Int, notification: Notification){
        val notificationManager = getNotificationManager(context)
        notificationManager?.notify(notificationId, notification)
    }

    fun cancelNotification(context: Context, notificationId: Int){
        val notificationManager = getNotificationManager(context)
        notificationManager?.cancel(notificationId)
    }

    private fun getNotificationManager(context: Context): NotificationManager? {
        return context.getSystemService()
    }

    private fun getAlarmManager(context: Context): AlarmManager? {
        return context.getSystemService()
    }

}