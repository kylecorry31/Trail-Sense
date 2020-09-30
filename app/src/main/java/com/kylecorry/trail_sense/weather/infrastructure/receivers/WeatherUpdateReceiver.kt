package com.kylecorry.trail_sense.weather.infrastructure.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import java.time.ZonedDateTime

class WeatherUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "Broadcast received at ${ZonedDateTime.now()}")
        if (context == null) {
            return
        }

        context.startService(WeatherUpdateService.intent(context))
    }

    companion object {

        private const val TAG = "WeatherUpdateReceiver"
        private const val INTENT_ACTION = "com.kylecorry.trail_sense.ALARM_UPDATE_WEATHER"
        const val PI_ID = 84097413

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                PI_ID,
                alarmIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        fun intent(context: Context): Intent {
            return Intent(context, WeatherUpdateReceiver::class.java)
        }

        private fun alarmIntent(context: Context): Intent {
            return IntentUtils.localIntent(context, INTENT_ACTION)
        }
    }
}