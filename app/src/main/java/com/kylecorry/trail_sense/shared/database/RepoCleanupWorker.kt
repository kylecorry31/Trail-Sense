package com.kylecorry.trail_sense.shared.database

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.IPeriodicTaskScheduler
import com.kylecorry.andromeda.jobs.PeriodicTaskSchedulerFactory
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trail_sense.weather.infrastructure.persistence.WeatherRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class RepoCleanupWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = UserPreferences(context)

        val backtrack = WaypointRepo.getInstance(context)
        backtrack.deleteOlderThan(Instant.now().minus(prefs.navigation.backtrackHistory))

        val weather = WeatherRepo.getInstance(context)
        weather.clean()

        Result.success()
    }


    companion object {
        fun scheduler(context: Context): IPeriodicTaskScheduler {
            return PeriodicTaskSchedulerFactory(context.applicationContext).deferrable(
                RepoCleanupWorker::class.java,
                2739523
            )
        }
    }

}