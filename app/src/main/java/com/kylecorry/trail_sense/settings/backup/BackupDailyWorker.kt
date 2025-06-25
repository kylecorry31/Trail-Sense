package com.kylecorry.trail_sense.settings.backup

import android.content.Context
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.background.DailyWorker
import com.kylecorry.andromeda.background.IOneTimeTaskScheduler
import com.kylecorry.andromeda.background.OneTimeTaskSchedulerFactory
import com.kylecorry.andromeda.files.ContentFileSystem
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

class BackupDailyWorker(context: Context, params: WorkerParameters) : DailyWorker(
    context,
    params,
    wakelockDuration = Duration.ofMinutes(1),
    tolerance = Duration.ofHours(6),
    getPreferences = { PreferencesSubsystem.getInstance(context).preferences },
) {

    override fun isEnabled(context: Context): Boolean {
        val prefs = UserPreferences(context)
        return prefs.backup.isAutoBackupEnabled
    }

    override fun getScheduledTime(context: Context): LocalTime {
        return LocalTime.of(22, 0)
    }

    override suspend fun execute(context: Context) {
        val prefs = UserPreferences(context)
        val uri = prefs.backup.autoBackupUri ?: return
        val contentFileSystem = ContentFileSystem(context, uri)

        if (!contentFileSystem.canWrite()) {
            BackupFailedAlerter(context).alert()
            return
        }

        val destination = contentFileSystem.createFile(
            "trail-sense-${Instant.now().epochSecond}.zip",
            "application/zip"
        ) ?: return

        // Backup
        val service = BackupService(context)
        service.backup(destination.uri)

        // Remove older files
        val allFiles = contentFileSystem.listFiles()
        val filesToDelete = allFiles
            .filter { it.name?.matches(Regex("trail-sense-\\d+.zip")) == true }
            .sortedByDescending { it.lastModified() }
            .drop(prefs.backup.autoBackupCount)

        filesToDelete.forEach {
            contentFileSystem.deleteFile(it.name ?: "")
        }
    }

    override val uniqueId: Int = UNIQUE_ID


    companion object {

        const val UNIQUE_ID = 21739812

        private fun getScheduler(context: Context): IOneTimeTaskScheduler {
            return OneTimeTaskSchedulerFactory(context).deferrable(
                BackupDailyWorker::class.java,
                UNIQUE_ID
            )
        }

        fun start(context: Context) {
            getScheduler(context).start()
        }

        fun stop(context: Context) {
            getScheduler(context).cancel()
        }
    }
}