package com.kylecorry.trail_sense.astronomy.infrastructure.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.background.AlarmBroadcastTaskScheduler
import com.kylecorry.andromeda.background.IOneTimeTaskScheduler
import com.kylecorry.andromeda.core.system.Intents
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

        Intents.startService(context, SunsetAlarmService.intent(context))
    }

    companion object {

        private const val PI_ID = 8309

        fun scheduler(context: Context): IOneTimeTaskScheduler {
            return AlarmBroadcastTaskScheduler(
                context.applicationContext,
                SunsetAlarmReceiver::class.java,
                PI_ID,
                exact = false,
                inexactWindow = Duration.ofMinutes(10),
                isWindowCentered = true,
                allowWhileIdle = true
            )
        }

        fun start(context: Context) {
            scheduler(context).start()
        }
    }
}