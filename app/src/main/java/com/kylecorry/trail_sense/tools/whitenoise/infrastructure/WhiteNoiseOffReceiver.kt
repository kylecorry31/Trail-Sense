package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WhiteNoiseOffReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        context.stopService(WhiteNoiseService.intent(context))
    }

    companion object {

        private const val PI_ID = 67832494

        fun intent(context: Context): Intent {
            return Intent(context, WhiteNoiseOffReceiver::class.java)
        }

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(context, PI_ID, intent(context), PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}