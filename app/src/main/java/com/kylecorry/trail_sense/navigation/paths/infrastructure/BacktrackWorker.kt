package com.kylecorry.trail_sense.navigation.paths.infrastructure

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.core.system.Wakelocks
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.jobs.IOneTimeTaskScheduler
import com.kylecorry.andromeda.jobs.OneTimeTaskSchedulerFactory
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.infrastructure.commands.BacktrackCommand
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration
import java.time.LocalDateTime


class BacktrackWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (requiresForeground()) {
            setForeground(
                ForegroundInfo(
                    73922,
                    Notify.background(
                        applicationContext,
                        NotificationChannels.CHANNEL_BACKGROUND_UPDATES,
                        applicationContext.getString(R.string.notification_backtrack_update_title),
                        applicationContext.getString(R.string.notification_backtrack_update_content),
                        R.drawable.ic_update
                    ),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    } else {
                        0
                    }
                )
            )
        }

        val wakelock = Wakelocks.get(applicationContext, WAKELOCK_TAG)
        tryOrNothing {
            wakelock?.acquire(Duration.ofSeconds(15).toMillis())
        }
        Log.d(javaClass.simpleName, "Started")
        try {
            BacktrackCommand(applicationContext).execute()
        } catch (e: Exception) {
            throw e
        } finally {
            val frequency = UserPreferences(applicationContext).backtrackRecordFrequency
            scheduler(applicationContext).once(frequency)
            Log.d(
                javaClass.simpleName,
                "Scheduled next run at ${LocalDateTime.now().plus(frequency)}"
            )
            wakelock?.release()

        }
        return Result.success()
    }

    private fun requiresForeground(): Boolean {
        return BacktrackRequiresForegroundSpecification().isSatisfiedBy(applicationContext)
    }

    companion object {
        private const val WAKELOCK_TAG = "com.kylecorry.trail_sense.BacktrackWorker:wakelock"

        fun scheduler(context: Context): IOneTimeTaskScheduler {
            return OneTimeTaskSchedulerFactory(context).deferrable(
                BacktrackWorker::class.java,
                7238542
            )
        }
    }

}