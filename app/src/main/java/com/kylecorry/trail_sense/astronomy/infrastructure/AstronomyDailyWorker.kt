package com.kylecorry.trail_sense.astronomy.infrastructure

import android.content.Context
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.DailyWorker
import com.kylecorry.andromeda.jobs.OneTimeTaskSchedulerFactory
import com.kylecorry.trail_sense.astronomy.infrastructure.commands.AstronomyAlertCommand
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration
import java.time.LocalTime

// TODO: Autogenerate last run key (pref_andromeda_daily_worker_last_run_date_UNIQUEID)
class AstronomyDailyWorker(context: Context, params: WorkerParameters) : DailyWorker(
    context,
    params,
    wakelockDuration = Duration.ofSeconds(15)
) {

    override fun isEnabled(context: Context): Boolean {
        val prefs = UserPreferences(context)
        return prefs.astronomy.sendAstronomyAlerts
    }

    override fun getScheduledTime(context: Context): LocalTime {
        val prefs = UserPreferences(context)
        return prefs.astronomy.astronomyAlertTime
    }

    override suspend fun execute(context: Context) {
        AstronomyAlertCommand(context).execute()
    }

    override val uniqueId: Int = UNIQUE_ID


    companion object {

        const val UNIQUE_ID = 72394823
        fun start(context: Context) {
            OneTimeTaskSchedulerFactory(context).deferrable(
                AstronomyDailyWorker::class.java,
                UNIQUE_ID
            ).start()
        }
    }
}