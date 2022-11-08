package com.kylecorry.trail_sense.astronomy.infrastructure

import android.content.Context
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.DailyWorker
import com.kylecorry.trail_sense.astronomy.infrastructure.commands.AstronomyAlertCommand
import com.kylecorry.trail_sense.shared.Background
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration
import java.time.LocalTime

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

    override val uniqueId: Int = Background.AstronomyAlerts
}