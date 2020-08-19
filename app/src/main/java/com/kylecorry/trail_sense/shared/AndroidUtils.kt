package com.kylecorry.trail_sense.shared

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.content.getSystemService
import java.time.LocalDateTime

object AndroidUtils {

    fun alarm(context: Context, time: LocalDateTime, pendingIntent: PendingIntent){
        val alarmManager = context.getSystemService<AlarmManager>()
        alarmManager?.setExact(AlarmManager.RTC_WAKEUP, time.toZonedDateTime().toEpochSecond() * 1000, pendingIntent)
    }

    fun cancelAlarm(context: Context, pendingIntent: PendingIntent){
        val alarmManager = context.getSystemService<AlarmManager>()
        alarmManager?.cancel(pendingIntent)
    }

}