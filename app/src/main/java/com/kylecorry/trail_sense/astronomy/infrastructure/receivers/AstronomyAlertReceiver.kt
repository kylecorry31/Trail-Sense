package com.kylecorry.trail_sense.astronomy.infrastructure.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.time.toZonedDateTime
import com.kylecorry.andromeda.jobs.ExactTaskScheduler
import com.kylecorry.andromeda.jobs.ITaskScheduler
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyAlertService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.getLocalDate
import com.kylecorry.trail_sense.shared.putLocalDate
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class AstronomyAlertReceiver : BroadcastReceiver() {

    // TODO: Extract this to a daily scheduler service
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val now = LocalDateTime.now()
        val prefs = UserPreferences(context)
        val cache = Preferences(context)
        val lastRun = cache.getLocalDate(LAST_RUN_DATE_KEY)
        val shouldSend = prefs.astronomy.sendAstronomyAlerts && lastRun != now.toLocalDate()

        val sendThreshold = Duration.ofMinutes(30)
        val sendTime = LocalDate.now().atTime(prefs.astronomy.astronomyAlertTime)
        val tomorrowSendTime =
            LocalDate.now().plusDays(1).atTime(prefs.astronomy.astronomyAlertTime)

        val sendWindowStart = sendTime - sendThreshold
        val sendWindowEnd = sendTime + sendThreshold

        val inWindow = now.isAfter(sendWindowStart) && now.isBefore(sendWindowEnd)
        val isTooEarly = now.isBefore(sendWindowStart)
        val isAfterWindow = now.isAfter(sendWindowEnd)

        if (inWindow && shouldSend) {
            Log.d(TAG, "Job started")
            cache.putLocalDate(LAST_RUN_DATE_KEY, now.toLocalDate())
            Intents.startService(context, AstronomyAlertService.intent(context), true)
            setAlarm(context, tomorrowSendTime)
        }

        if (isTooEarly) {
            Log.d(TAG, "Too early")
            setAlarm(context, sendTime)
        }

        if (isAfterWindow || (inWindow && !shouldSend)) {
            Log.d(TAG, "Too late / can't send")
            setAlarm(context, tomorrowSendTime)
        }
    }

    private fun setAlarm(context: Context, time: LocalDateTime) {
        val scheduler = scheduler(context)
        scheduler.cancel()
        scheduler.schedule(time.toZonedDateTime().toInstant())
        Log.i(TAG, "Set next astronomy alert at $time")
    }

    companion object {

        private const val TAG = "AstronomyAlertReceiver"
        const val LAST_RUN_DATE_KEY = "pref_astronomy_alerts_last_run_date"
        private const val PI_ID = 72634

        fun scheduler(context: Context): ITaskScheduler {
            return ExactTaskScheduler(context) { pendingIntent(context) }
        }

        fun intent(context: Context): Intent {
            return Intent(context, AstronomyAlertReceiver::class.java)
        }

        private fun alarmIntent(context: Context): Intent {
            return Intents.localIntent(context, "com.kylecorry.trail_sense.ASTRONOMY_ALERT")
        }

        fun pendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                PI_ID,
                alarmIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}