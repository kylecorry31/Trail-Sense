package com.kylecorry.trail_sense.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            getAppService<Logger>().debug("BootReceiver", "Boot completed")
            val pendingResult = goAsync()
            TrailSenseServiceUtils.restartServices(
                context,
                isInBackground = true,
                onComplete = pendingResult::finish
            )
        }
    }
}
