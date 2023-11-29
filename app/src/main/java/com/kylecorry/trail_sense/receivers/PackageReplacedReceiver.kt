package com.kylecorry.trail_sense.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED && context != null) {
            Log.d("PackageReplacedReceiver", "Package replaced")
            TrailSenseServiceUtils.restartServices(context, true)
        }
    }
}