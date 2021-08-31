package com.kylecorry.trail_sense.astronomy.infrastructure

import android.content.Context
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.DailyWorker
import com.kylecorry.andromeda.jobs.WorkTaskScheduler
import com.kylecorry.trail_sense.astronomy.infrastructure.commands.AstronomyAlertCommand
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration
import java.time.LocalTime

class AstronomyDailyWorker(context: Context, params: WorkerParameters) : DailyWorker(
    context,
    params
) {

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

    override suspend fun execute(context: Context) {
        AstronomyAlertCommand(context).execute()
    }

    override val uniqueId: String = UNIQUE_ID


    companion object {
        private const val UNIQUE_ID = "com.kylecorry.trail_sense.astronomy.AstronomyDailyWorker"
        fun start(context: Context) {
            WorkTaskScheduler(context, AstronomyDailyWorker::class.java, UNIQUE_ID, false).schedule(
                Duration.ZERO
            )
        }
    }
}