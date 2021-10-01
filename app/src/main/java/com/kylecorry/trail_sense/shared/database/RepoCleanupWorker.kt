package com.kylecorry.trail_sense.shared.database

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.ITaskScheduler
import com.kylecorry.andromeda.jobs.TaskSchedulerFactory
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trail_sense.weather.infrastructure.clouds.CloudObservationRepo
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

class RepoCleanupWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = UserPreferences(context)

        val clouds = CloudObservationRepo.getInstance(context)
        clouds.clean()

        val backtrack = WaypointRepo.getInstance(context)
        backtrack.deleteOlderThan(Instant.now().minus(prefs.navigation.backtrackHistory))

        val pressure = PressureRepo.getInstance(context)
        pressure.deleteOlderThan(Instant.now().minus(PressureRepo.PRESSURE_HISTORY_DURATION))

        scheduler(context).schedule(Duration.ofDays(1))

        Result.success()
    }


    companion object {
        fun scheduler(context: Context): ITaskScheduler {
            return TaskSchedulerFactory(context.applicationContext).deferrable(
                RepoCleanupWorker::class.java,
                2739523
            )
        }
    }

}