package com.kylecorry.trail_sense.settings.backup

import android.content.Context
import android.net.Uri
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.system.AppData
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.files.CacheFileSystem
import com.kylecorry.andromeda.files.ZipFile
import com.kylecorry.andromeda.files.ZipUtils
import com.kylecorry.andromeda.files.ZipUtils.unzip
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.receivers.TrailSenseServiceUtils
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance.CachedTileRepo
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
        val filesToBackup = getFilesToBackup().toMutableList()

        val appVersionFile =
            File(context.filesDir, "app-version-${Package.getVersionCode(context)}.txt")
        appVersionFile.createNewFile()
        filesToBackup.add(appVersionFile)

        try {
            // Create a DB checkpoint to avoid losing data
            AppDatabase.createCheckpoint(context)

            val excludedFiles = listOf(
                fileSubsystem.getDirectory("dem"),
                AppData.getSharedPrefsFile(context, "${context.packageName}_widget_preferences")
            )

            // Create the zip file
            fileSubsystem.output(destination)?.use {
                ZipUtils.zip(it, *filesToBackup.toTypedArray(), excludedFiles = excludedFiles)
            }
        } finally {
            // Delete the app version file
            appVersionFile.delete()
        }
    }

    /**
     * Restores the app data from the given source zip file
     * @param source the source file - must be a zip file
     */
    suspend fun restore(source: Uri, onProgress: ((Float) -> Unit)? = null): Unit = onIO {
        // Check the validity of the zip file
        val backup = verifyBackupFile(source)

        // Get the root directory where the files will be restored to
        val root = AppData.getDataDirectory(context)

        // Stop the services while restoring to prevent DB corruption
        TrailSenseServiceUtils.stopServices(context)

        // Close the DB before restoring
        AppDatabase.close()

        // Remove the shared prefs directory (this is to support switching between nightly, dev, and regular builds)
        AppData.getSharedPrefsDirectory(context).deleteRecursively()

        // Unzip the files to the root directory (this will overwrite existing files)
        var count = 0
        fileSubsystem.stream(source)?.use {
            ZipUtils.unzip(it, root, MAX_ZIP_FILE_COUNT) {
                count++
                onProgress?.invoke(count / backup.fileCount.coerceAtLeast(1).toFloat())
            }
        } ?: return@onIO

        // Rename the shared prefs file
        renameSharedPrefsFile(backup.sharedPrefsFileName)

        // Clear cache
        CacheFileSystem(context).delete(CachedTileRepo.TILES_FOLDER, true)
        onProgress?.invoke(1f)
    }

    private suspend fun renameSharedPrefsFile(name: String?): Unit = onIO {
        if (name == null) {
            return@onIO
        }

        val sharedPrefsDir = AppData.getSharedPrefsDirectory(context)

        // Get the restored shared preferences file from the backup.
        val prefsFile = File(sharedPrefsDir, name)
        if (!prefsFile.exists()) {
            return@onIO
        }

        // Rename it to match the current package name (allows switching between nightly, dev, and regular builds)
        val target = File(sharedPrefsDir, "${context.packageName}_preferences.xml")
        if (prefsFile.absolutePath == target.absolutePath) {
            return@onIO
        }

        if (target.exists()) {
            target.delete()
        }

        prefsFile.renameTo(target)
    }

    private suspend fun verifyBackupFile(backupUri: Uri): BackupMetadata = onIO {
        // Retrieve the files in the zip file
        val files = fileSubsystem.stream(backupUri)?.use {
            ZipUtils.list(it, MAX_ZIP_FILE_COUNT)
        } ?: throw InvalidBackupException()

        // If the app version file doesn't exist or the version code is greater than the current version, return false
        val appVersionFile =
            files.firstOrNull { (file, _) -> file.path.contains("app-version") }
        val version = appVersionFile?.let { extractVersionCode(it.file.path) }
        if (version == null || version > Package.getVersionCode(context)) {
            throw NewerBackupException()
        }

        // Verify it looks like a backup
        val pathsToLookFor = listOf(
            Regex("databases/trail_sense.*"),
            Regex("shared_prefs/com\\.kylecorry\\.trail_sense.*_preferences.xml"),
        )
        if (files.none { (file, _) -> pathsToLookFor.any { re -> re.matches(file.path) } }) {
            throw InvalidBackupException()
        }

        BackupMetadata(files.size, getSharedPrefsFileName(files))
    }

    private fun getSharedPrefsFileName(files: List<ZipFile>): String? {
        return files.firstOrNull { (file, isDirectory) ->
            !isDirectory &&
                    !file.name.contains("widget_preferences") &&
                    SHARED_PREFS_REGEX.matches(file.path)
        }?.file?.name
    }

    private fun getFilesToBackup(): List<File> {
        val files = AppData.getFilesDirectory(context)
        val database = AppData.getDatabaseDirectory(
            context,
            "trail_sense"
        )
        val sharedPrefsDir = AppData.getSharedPrefsDirectory(context)
        return listOfNotNull(files, database, sharedPrefsDir)
    }

    private fun extractVersionCode(path: String): Long? {
        val regex = Regex("app-version-(\\d+).txt")
        val match = regex.find(path) ?: return null
        return match.groupValues[1].toLongOrNull()
    }

    class InvalidBackupException : Exception()
    class NewerBackupException : Exception()

    private data class BackupMetadata(
        val fileCount: Int,
        val sharedPrefsFileName: String?
    )

    companion object {
        private const val MAX_ZIP_FILE_COUNT = 1000
        private val SHARED_PREFS_REGEX =
            Regex("shared_prefs/com\\.kylecorry\\.trail_sense.*_preferences.xml")
    }

}
