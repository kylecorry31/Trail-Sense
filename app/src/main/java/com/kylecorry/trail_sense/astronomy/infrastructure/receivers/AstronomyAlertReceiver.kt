package com.kylecorry.trail_sense.astronomy.infrastructure.receivers

import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.jobs.DailyJobReceiver
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyAlertService
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.LocalTime

class AstronomyAlertReceiver : DailyJobReceiver() {

    override fun isEnabled(context: Context): Boolean {
        val prefs = UserPreferences(context)
        return prefs.astronomy.sendAstronomyAlerts
    }

    override fun getScheduledTime(context: Context): LocalTime {
        val prefs = UserPreferences(context)
        return prefs.astronomy.astronomyAlertTime
    }

    override fun getLastRunKey(context: Context): String {
        return "pref_astronomy_alerts_last_run_date"
    }

    override fun execute(context: Context) {
        Intents.startService(context, AstronomyAlertService.intent(context), true)
    }

    override val pendingIntentId: Int
        get() = 72634

    companion object {
        fun start(context: Context) {
            context.sendBroadcast(Intent(context, AstronomyAlertReceiver::class.java))
        }
    }
}