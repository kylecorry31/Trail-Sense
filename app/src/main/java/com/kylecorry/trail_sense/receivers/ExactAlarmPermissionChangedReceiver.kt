package com.kylecorry.trail_sense.receivers

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ExactAlarmPermissionChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED && context != null) {
            Log.d("ExactAlarmPermissionChangedReceiver", "Exact alarm permission changed")
            RestartServicesCommand(context, true).execute()
        }
    }
}