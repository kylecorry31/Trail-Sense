package com.kylecorry.trail_sense.tools.paths.infrastructure.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.StopBacktrackCommand

class StopBacktrackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        StopBacktrackCommand(context).execute()
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, StopBacktrackReceiver::class.java)
        }

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                67293,
                intent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

}