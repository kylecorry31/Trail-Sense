package com.kylecorry.trail_sense.main.persistence

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.background.IPeriodicTaskScheduler
import com.kylecorry.andromeda.background.TaskSchedulerFactory
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.ILightningRepo
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class RepoCleanupWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    val lightningRepo: ILightningRepo
) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        Log.d("RepoCleanupWorker", "Cleaning up repositories")

        val cleanables: List<ICleanable> = listOf(
            PathService.getInstance(context),
            WeatherRepo.getInstance(context),
            CloudRepo.getInstance(context),
            lightningRepo
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