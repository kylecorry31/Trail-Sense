package com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.background.IOneTimeTaskScheduler
import com.kylecorry.andromeda.background.OneTimeTaskSchedulerFactory
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.RequestBackgroundLocationCommand
import com.kylecorry.trail_sense.shared.permissions.requestScheduleExactAlarms
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands.SunsetAlarmCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SunsetAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val shouldSend = UserPreferences(context).astronomy.sendSunsetAlerts
        if (!shouldSend) {
            return
        }

        val pendingResult = goAsync()

        val command = SunsetAlarmCommand(context.applicationContext)
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                command.execute()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {

        private const val PI_ID = 8309

        fun scheduler(context: Context): IOneTimeTaskScheduler {
            return OneTimeTaskSchedulerFactory(context).exact(
                SunsetAlarmReceiver::class.java,
                PI_ID
            )
        }

        fun start(context: Context) {
            context.sendBroadcast(Intent(context, SunsetAlarmReceiver::class.java))
        }

        // TODO: Extract this out of the receiver
        /**
         * Enable sunset alerts and request permissions if needed
         */
        suspend fun <T> enable(
            fragment: T,
            shouldRequestPermissions: Boolean
        ) where T : Fragment, T : IPermissionRequester {
            val service = Tools.getService(
                fragment.requireContext(),
                AstronomyToolRegistration.SERVICE_SUNSET_ALERTS
            )
            service?.enable()
            if (shouldRequestPermissions) {
                fragment.requestScheduleExactAlarms {
                    runBlocking {
                        service?.restart()
                    }
                    RequestBackgroundLocationCommand(fragment).execute()
                }
            }
        }
    }
}