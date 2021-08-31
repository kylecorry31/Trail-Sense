package com.kylecorry.trail_sense.astronomy.infrastructure.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.jobs.AlarmBroadcastTaskScheduler
import com.kylecorry.andromeda.jobs.ITaskScheduler
import com.kylecorry.trail_sense.astronomy.infrastructure.SunsetAlarmService
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration

class SunsetAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val shouldSend = UserPreferences(context).astronomy.sendSunsetAlerts
        if (!shouldSend) {
            return
        }

        Intents.startService(context, SunsetAlarmService.intent(context), true)
    }

    companion object {

        private const val PI_ID = 8309

        fun scheduler(context: Context): ITaskScheduler {
            return AlarmBroadcastTaskScheduler(context, SunsetAlarmReceiver::class.java, PI_ID)
        }

        fun start(context: Context) {
            scheduler(context).schedule(Duration.ZERO)
        }
    }
}