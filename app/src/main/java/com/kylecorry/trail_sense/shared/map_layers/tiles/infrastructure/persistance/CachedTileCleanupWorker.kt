package com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.background.IOneTimeTaskScheduler
import com.kylecorry.andromeda.background.OneTimeTaskSchedulerFactory

class CachedTileCleanupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            CachedTileRepo.getInstance(context).clean()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val UNIQUE_ID = 91420833

        private fun getScheduler(context: Context): IOneTimeTaskScheduler {
            return OneTimeTaskSchedulerFactory(context.applicationContext).deferrable(
                CachedTileCleanupWorker::class.java,
                UNIQUE_ID
            )
        }

        fun start(context: Context) {
            getScheduler(context).start()
        }
    }
}
