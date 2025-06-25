package com.kylecorry.trail_sense.main.persistence

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.background.IPeriodicTaskScheduler
import com.kylecorry.andromeda.background.TaskSchedulerFactory
import com.kylecorry.trail_sense.shared.dem.DEMRepo
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RepoCleanupWorker(
    private val context: Context,
    params: WorkerParameters
) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        Log.d("RepoCleanupWorker", "Cleaning up repositories")

        val cleanables: List<ICleanable> = listOf(
            PathService.getInstance(context),
            WeatherRepo.getInstance(context),
            CloudRepo.getInstance(context),
            LightningRepo.getInstance(context),
            DEMRepo.getInstance()
        )

        for (repo in cleanables) {
            repo.clean()
        }

        DeleteTempFilesCommand(context).execute()

        Log.d("RepoCleanupWorker", "Finished cleaning up repositories")

        Result.success()
    }


    companion object {
        fun scheduler(context: Context): IPeriodicTaskScheduler {
            return TaskSchedulerFactory(context.applicationContext).interval(
                RepoCleanupWorker::class.java,
                2739523
            )
        }
    }

}