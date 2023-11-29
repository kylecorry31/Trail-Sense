package com.kylecorry.trail_sense.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            Log.d("BootReceiver", "Boot completed")
            TrailSenseServiceUtils.restartServices(context, isInBackground = true)
        }
    }
}