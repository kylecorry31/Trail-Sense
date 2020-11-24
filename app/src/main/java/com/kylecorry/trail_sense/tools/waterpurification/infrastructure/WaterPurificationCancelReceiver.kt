package com.kylecorry.trail_sense.tools.waterpurification.infrastructure

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WaterPurificationCancelReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        WaterPurificationTimerService.stop(context)
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, WaterPurificationCancelReceiver::class.java)
        }

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(context, 21830948, intent(context), PendingIntent.FLAG_CANCEL_CURRENT)
        }
    }

}