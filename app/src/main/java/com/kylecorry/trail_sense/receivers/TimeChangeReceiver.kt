package com.kylecorry.trail_sense.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger

class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val validIntentActions = listOf(
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED
        )
        if (validIntentActions.contains(intent?.action) && context != null) {
            getAppService<Logger>().info("TimeChangeReceiver", "${intent?.action}, restarting services")
            val pendingResult = goAsync()
            TrailSenseServiceUtils.restartServices(
                context,
                isInBackground = true,
                onComplete = pendingResult::finish
            )
        }
    }
}
