package com.kylecorry.trail_sense.astronomy.infrastructure.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.astronomy.infrastructure.SunsetAlarmService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils

class SunsetAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val shouldSend = UserPreferences(context).astronomy.sendSunsetAlerts
        if (!shouldSend) {
            return
        }

        IntentUtils.startService(context, SunsetAlarmService.intent(context), true)
    }

    companion object {

        private const val PI_ID = 8309

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