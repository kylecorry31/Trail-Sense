package com.kylecorry.trail_sense.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.files.ZipUtils
import com.kylecorry.trail_sense.receivers.TrailSenseServiceUtils
import com.kylecorry.trail_sense.shared.database.AppDatabase
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import kotlinx.coroutines.delay
import java.io.File

class BackupService(
    private val context: Context,
    private val fileSubsystem: FileSubsystem = FileSubsystem.getInstance(context)
) {

    /**
     * Backs up the app data to the given destination zip file
     * @param destination the destination file - must be a zip file
     */
    suspend fun backup(destination: Uri): Unit = onIO {
        // Get the files to backup
        val filesToBackup = getFilesToBackup()

        // Stop the services while backing up to prevent DB corruption
        TrailSenseServiceUtils.stopServices(context)

        try {
            // Close the DB before backing up
            AppDatabase.close()

            // Create the zip file
            fileSubsystem.output(destination)?.use {
                ZipUtils.zip(it, *filesToBackup.toTypedArray())
            }
        } finally {
            // Restart the services
            TrailSenseServiceUtils.restartServices(context)
        }
    }

    /**
     * Restores the app data from the given source zip file
     * @param source the source file - must be a zip file
     */
    suspend fun restore(source: Uri): Unit = onIO {
        // Get the root directory where the files will be restored to
        val root = getDataDir() ?: return@onIO

        // Check the validity of the zip file
        if (!isBackupValid(source)){
            throw InvalidBackupException()
        }

        // Stop the services while restoring to prevent DB corruption
        TrailSenseServiceUtils.stopServices(context)

        // Close the DB before restoring
        AppDatabase.close()

        // Remove the shared prefs directory (this is to support switching between nightly, dev, and regular builds)
        getSharedPrefsDir()?.deleteRecursively()

        // Unzip the files to the root directory (this will overwrite existing files)
        fileSubsystem.stream(source)?.use {
            ZipUtils.unzip(it, root, MAX_ZIP_FILE_COUNT)
        } ?: return@onIO

        // Rename the shared prefs file
        renameSharedPrefsFile()

        // App is going to restart after this is done, so don't restart the services
    }

    private suspend fun renameSharedPrefsFile(): Unit = onIO {
        val sharedPrefsDir = getSharedPrefsDir()
        // Get the xml file from that directory
        val prefsFile = getSharedPrefsFile() ?: return@onIO
        // Rename it to match the current package name (allows switching between nightly, dev, and regular builds)
        prefsFile.renameTo(File(sharedPrefsDir, "${context.packageName}_preferences.xml"))
    }

    private suspend fun isBackupValid(backupUri: Uri): Boolean = onIO {
        val pathsToLookFor = listOf(
            Regex("databases/trail_sense.*"),
            Regex("shared_prefs/com\\.kylecorry\\.trail_sense.*_preferences.xml"),
        )
        fileSubsystem.stream(backupUri)?.use {
            val files = ZipUtils.list(it, MAX_ZIP_FILE_COUNT)
            files.any { (file, _) -> pathsToLookFor.any { re -> re.matches(file.path) } }
        } ?: false
    }

    private fun getFilesToBackup(): List<File> {
        val files = context.filesDir
        val databases = getDatabaseDir()
        val sharedPrefsDir = getSharedPrefsDir()
        return listOfNotNull(files, databases, sharedPrefsDir)
    }

    private fun getDatabaseDir(): File? {
        return context.getDatabasePath("trail_sense")?.parentFile
    }

    private fun getSharedPrefsDir(): File? {
        return getDataDir()?.resolve("shared_prefs")
    }

    private fun getSharedPrefsFile(): File? {
        return getSharedPrefsDir()?.listFiles()?.firstOrNull { it.extension == "xml" }
    }

    private fun getDataDir(): File? {
        return ContextCompat.getDataDir(context) ?: context.filesDir.parentFile
    }

    class InvalidBackupException: Exception()

    companion object {
        private const val MAX_ZIP_FILE_COUNT = 1000
    }

}