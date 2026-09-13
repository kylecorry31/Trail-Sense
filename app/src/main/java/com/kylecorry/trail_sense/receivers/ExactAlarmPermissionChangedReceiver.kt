package com.kylecorry.trail_sense.receivers

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger

class ExactAlarmPermissionChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED && context != null) {
            getAppService<Logger>().info(
                "ExactAlarmPermissionChangedReceiver",
                "Exact alarm permission changed, restarting services"
            )
            RestartServicesCommand(context, true).execute()
        }
    }
}
