package com.kylecorry.trail_sense.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val validIntentActions = listOf(
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED
        )
        if (validIntentActions.contains(intent?.action) && context != null) {
            Log.d("TimeChangeReceiver", "Time Change Receiver Called - ${intent?.action}")
            TrailSenseServiceUtils.restartServices(context)
        }
    }
}