package com.kylecorry.trail_sense.tools.backtrack.infrastructure

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.ITaskScheduler
import com.kylecorry.andromeda.jobs.WorkTaskScheduler
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.commands.BacktrackCommand
import java.time.LocalDateTime


class BacktrackWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d(javaClass.simpleName, "Started")
        try {
            setForeground(createForegroundInfo(applicationContext))
            BacktrackCommand(applicationContext).execute()
        } catch (e: Exception) {
            throw e
        } finally {
            val frequency = UserPreferences(applicationContext).backtrackRecordFrequency
            scheduler(applicationContext).schedule(frequency)
            Log.d(javaClass.simpleName, "Scheduled next job at ${LocalDateTime.now().plus(frequency)}")

        }
        return Result.success()
    }

    private fun createForegroundInfo(context: Context): ForegroundInfo {
        val notification = Notify.background(
            context,
            NotificationChannels.CHANNEL_BACKGROUND_UPDATES,
            context.getString(R.string.backtrack),
            context.getString(R.string.backtrack_notification_description),
            R.drawable.ic_update,
            group = NotificationChannels.GROUP_UPDATES
        )

        val notificationId = 76984343

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    companion object {
        private const val WORK_TAG = "com.kylecorry.trail_sense.BacktrackWorker"

        fun scheduler(context: Context): ITaskScheduler {
            return WorkTaskScheduler(
                context,
                BacktrackWorker::class.java,
                WORK_TAG,
                false
            )
        }
    }

}