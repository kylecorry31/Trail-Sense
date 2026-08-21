package com.kylecorry.trail_sense.settings.migrations

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.background.IOneTimeTaskScheduler
import com.kylecorry.andromeda.background.WorkTaskScheduler
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackerService

class PedometerPreferenceMigrationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val migration = PedometerPreferenceMigration(
        getAppService<StepTrackerService>(),
        getAppService<PreferencesSubsystem>().preferences
    )

    override suspend fun doWork(): Result {
        try {
            migration.migrate()
        } catch (_: Exception) {
            return if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                // Accept that this can't be migrated - there's not much data loss
                Result.failure()
            }
        }

        return Result.success()
    }

    companion object {
        private const val UNIQUE_ID = 68103245
        private const val MAX_RETRY_ATTEMPTS = 3

        private fun getScheduler(context: Context): IOneTimeTaskScheduler {
            return WorkTaskScheduler(
                context.applicationContext,
                PedometerPreferenceMigrationWorker::class.java,
                WorkTaskScheduler.createStringId(context, UNIQUE_ID),
                // Below Android 12, expedited work runs as a foreground service and requires the
                // worker to provide a notification, which isn't worth it for this migration
                expedited = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            )
        }

        fun start(context: Context) {
            getScheduler(context).start()
        }
    }
}
