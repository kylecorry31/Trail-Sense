package com.kylecorry.trail_sense.shared

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService
import java.time.LocalDateTime

object SystemUtils {

    fun alarm(context: Context, time: LocalDateTime, pendingIntent: PendingIntent){
        val alarmManager = context.getSystemService<AlarmManager>()
        alarmManager?.setExact(AlarmManager.RTC_WAKEUP, time.toEpochMillis(), pendingIntent)
    }

    fun cancelAlarm(context: Context, pendingIntent: PendingIntent){
        try {
            val alarmManager = context.getSystemService<AlarmManager>()
            alarmManager?.cancel(pendingIntent)
        } catch (e: Exception){
            Log.e("SystemUtils", "Could not cancel alarm", e)
        }
    }

}