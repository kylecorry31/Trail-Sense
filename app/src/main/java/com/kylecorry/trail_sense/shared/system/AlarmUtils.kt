package com.kylecorry.trail_sense.shared.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.shared.toEpochMillis
import java.time.LocalDateTime

object AlarmUtils {

    /**
     * Create an alarm
     * @param context The context
     * @param time The time to fire the alarm
     * @param pendingIntent The pending intent to launch when the alarm fires
     * @param exact True if the alarm needs to fire at exactly the time specified, false otherwise
     */
    fun set(
        context: Context,
        time: LocalDateTime,
        pendingIntent: PendingIntent,
        exact: Boolean = true,
        allowWhileIdle: Boolean = false
    ) {
        val alarmManager = getAlarmManager(context)
        if (!allowWhileIdle) {
            if (exact) {
                alarmManager?.setExact(AlarmManager.RTC_WAKEUP, time.toEpochMillis(), pendingIntent)
            } else {
                alarmManager?.set(AlarmManager.RTC_WAKEUP, time.toEpochMillis(), pendingIntent)
            }
        } else {
            if (exact) {
                alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.toEpochMillis(), pendingIntent)
            } else {
                alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.toEpochMillis(), pendingIntent)
            }
        }
    }

    /**
     * Cancel the alarm associated with the pending intent
     * @param context The context
     * @param pendingIntent The pending intent to cancel
     */
    fun cancel(context: Context, pendingIntent: PendingIntent) {
        try {
            val alarmManager = getAlarmManager(context)
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            Log.e("SystemUtils", "Could not cancel alarm", e)
        }
    }

    /**
     * Determines if an alarm is running
     * @param context The context
     * @param requestCode The request code of the pending intent
     * @param intent The intent used for the pending intent
     * @return true if the alarm is running, false otherwise
     */
    fun isAlarmRunning(context: Context, requestCode: Int, intent: Intent): Boolean {
        return IntentUtils.pendingIntentExists(context, requestCode, intent)
    }

    private fun getAlarmManager(context: Context): AlarmManager? {
        return context.getSystemService()
    }

}