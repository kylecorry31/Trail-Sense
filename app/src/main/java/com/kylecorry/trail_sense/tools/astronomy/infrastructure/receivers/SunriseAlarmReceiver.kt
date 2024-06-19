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
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands.SunriseAlarmCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SunriseAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val shouldSend = UserPreferences(context).astronomy.sendSunriseAlerts
        if (!shouldSend) {
            return
        }

        val pendingResult = goAsync()

        val command = SunriseAlarmCommand(context.applicationContext)
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

        private const val PI_ID = 8310

        fun scheduler(context: Context): IOneTimeTaskScheduler {
            return OneTimeTaskSchedulerFactory(context).exact(
                SunriseAlarmReceiver::class.java,
                PI_ID
            )
        }

        fun start(context: Context) {
            context.sendBroadcast(Intent(context, SunriseAlarmReceiver::class.java))
        }

        // TODO: Extract this out of the receiver
        /**
         * Enable sunrise alerts and request permissions if needed
         */
        fun <T> enable(
            fragment: T,
            shouldRequestPermissions: Boolean
        ) where T : Fragment, T : IPermissionRequester {
            UserPreferences(fragment.requireContext()).astronomy.sendSunriseAlerts = true
            if (shouldRequestPermissions) {
                fragment.requestScheduleExactAlarms {
                    start(fragment.requireContext())
                    RequestBackgroundLocationCommand(fragment).execute()
                }
            } else {
                start(fragment.requireContext())
            }
        }
    }
}