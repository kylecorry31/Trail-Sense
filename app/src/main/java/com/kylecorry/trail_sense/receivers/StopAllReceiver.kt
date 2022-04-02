package com.kylecorry.trail_sense.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.navigation.paths.infrastructure.commands.StopBacktrackCommand
import com.kylecorry.trail_sense.weather.infrastructure.commands.StopWeatherMonitorCommand

class StopAllReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        StopWeatherMonitorCommand(context).execute()
        StopBacktrackCommand(context).execute()
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, StopAllReceiver::class.java)
        }

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                724392,
                intent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}