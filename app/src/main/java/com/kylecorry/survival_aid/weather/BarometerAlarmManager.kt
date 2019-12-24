package com.kylecorry.survival_aid.weather

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object BarometerAlarmManager {

    private var alarmMgr: AlarmManager? = null
    private lateinit var alarmIntent: PendingIntent
    private var started: Boolean = false

    fun startRecording(context: Context){
        if (started) return
        started = true
        alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmIntent = Intent(context, BarometerAlarmReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(context, 0, intent, 0)
        }
        alarmMgr?.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime(),
            AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            alarmIntent
        )
    }

    fun stopRecording(){
        if (!started) return
        started = false
        alarmMgr?.cancel(alarmIntent)
    }



}