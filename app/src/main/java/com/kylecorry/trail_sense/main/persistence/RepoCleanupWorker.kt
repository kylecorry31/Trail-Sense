package com.kylecorry.trail_sense.main.persistence

import android.content.Context
import android.os.SystemClock
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.background.IPeriodicTaskScheduler
import com.kylecorry.andromeda.background.TaskSchedulerFactory
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dem.DEMRepo
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance.CachedTileRepo
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearingService
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackerService
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RepoCleanupWorker(
    private val context: Context,
    params: WorkerParameters
) :
    CoroutineWorker(context, params) {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val logger = getAppService<Logger>()
        val start = SystemClock.elapsedRealtime()

        // Named explicitly because class names are obfuscated in release builds
        val cleanables: List<Pair<String, ICleanable>> = listOf(
            "PathService" to PathService.getInstance(context),
            "WeatherRepo" to WeatherRepo.getInstance(context),
            "CloudRepo" to CloudRepo.getInstance(context),
            "LightningRepo" to LightningRepo.getInstance(context),
            "DEMRepo" to DEMRepo.getInstance(),
            "NavigationBearingService" to NavigationBearingService.getInstance(context),
            "CachedTileRepo" to CachedTileRepo.getInstance(context),
            "StepTrackerService" to getAppService<StepTrackerService>()
        )

        // One failing cleanup should not prevent the rest from running
        var failures = 0
        for ((name, repo) in cleanables) {
            try {
                repo.clean()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failures++
                logger.error(TAG, "Unable to clean $name", e)
            }
        }

        getAppService<OfflineMapService>().cleanup()
        DeleteTempFilesCommand(context).execute()

        logger.info(
            TAG,
            "Cleaned up repositories in ${SystemClock.elapsedRealtime() - start}ms with $failures failures"
        )

        Result.success()
    }


    companion object {
        private const val TAG = "RepoCleanupWorker"

        fun scheduler(context: Context): IPeriodicTaskScheduler {
            return TaskSchedulerFactory(context.applicationContext).interval(
                RepoCleanupWorker::class.java,
                2739523
            )
        }
    }

}
