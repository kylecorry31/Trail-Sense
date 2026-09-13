package com.kylecorry.trail_sense.settings.backup

import android.content.Context
import android.os.SystemClock
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.background.DailyWorker
import com.kylecorry.andromeda.background.IOneTimeTaskScheduler
import com.kylecorry.andromeda.background.OneTimeTaskSchedulerFactory
import com.kylecorry.andromeda.files.ContentFileSystem
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import kotlinx.coroutines.CancellationException
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

    @Suppress("TooGenericExceptionCaught")
    override suspend fun execute(context: Context) {
        val prefs = UserPreferences(context)
        val logger = getAppService<Logger>()
        val uri = prefs.backup.autoBackupUri
        if (uri == null) {
            logger.warn(TAG, "Automatic backup is enabled but no directory is set")
            return
        }
        val contentFileSystem = ContentFileSystem(context, uri)

        if (!contentFileSystem.canWrite()) {
            logger.warn(TAG, "Unable to write to the automatic backup directory, alerting user")
            BackupFailedAlerter(context).alert()
            return
        }

        val destination = contentFileSystem.createFile(
            "trail-sense-${Instant.now().epochSecond}.zip",
            "application/zip"
        )
        if (destination == null) {
            logger.warn(TAG, "Unable to create the automatic backup file")
            return
        }

        // Backup
        val start = SystemClock.elapsedRealtime()
        val service = BackupService(context)
        try {
            service.backup(destination.uri)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(TAG, "Automatic backup failed", e)
            throw e
        }

        // Remove older files
        val allFiles = contentFileSystem.listFiles()
        val filesToDelete = allFiles
            .filter { it.name?.matches(Regex("trail-sense-\\d+.zip")) == true }
            .sortedByDescending { it.lastModified() }
            .drop(prefs.backup.autoBackupCount)

        filesToDelete.forEach {
            contentFileSystem.deleteFile(it.name ?: "")
        }

        logger.info(
            TAG,
            "Automatic backup created in ${SystemClock.elapsedRealtime() - start}ms, deleted ${filesToDelete.size} old backups"
        )
    }

    override val uniqueId: Int = UNIQUE_ID


    companion object {

        const val UNIQUE_ID = 21739812
        private const val TAG = "BackupDailyWorker"

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
