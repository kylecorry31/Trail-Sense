package com.kylecorry.trail_sense.astronomy.infrastructure.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.background.IOneTimeTaskScheduler
import com.kylecorry.andromeda.background.OneTimeTaskSchedulerFactory
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.trail_sense.astronomy.infrastructure.SunsetAlarmService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.RequestBackgroundLocationCommand
import com.kylecorry.trail_sense.shared.permissions.requestScheduleExactAlarms

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
        fun <T> enable(
            fragment: T,
            shouldRequestPermissions: Boolean
        ) where T : Fragment, T : IPermissionRequester {
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