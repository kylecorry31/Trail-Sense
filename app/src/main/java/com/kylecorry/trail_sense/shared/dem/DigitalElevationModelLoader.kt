package com.kylecorry.trail_sense.shared.dem

import android.net.Uri
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.files.ZipUtils
import com.kylecorry.trail_sense.shared.io.FileSubsystem

class DigitalElevationModelLoader {

    suspend fun load(source: Uri) = onIO {
        val files = AppServiceRegistry.get<FileSubsystem>()

        // Validate the source URI
        files.stream(source)?.use {
            if (!ZipUtils.list(it, MAX_ZIP_FILE_COUNT).any { it.file.name == "index.json" }) {
                throw IllegalArgumentException("The provided zip file does not contain a valid DEM index.json file.")
            }
        }

        // Clear the existing files
        files.getDirectory("dem", create = true).deleteRecursively()

        val destination = files.getDirectory("dem", create = true)

        // Unzip the files
        files.stream(source)?.use {
            ZipUtils.unzip(it, destination, MAX_ZIP_FILE_COUNT)
        } ?: return@onIO

        DEM.invalidateCache()
    }

    suspend fun clear() = onIO {
        val files = AppServiceRegistry.get<FileSubsystem>()
        files.getDirectory("dem", create = true).deleteRecursively()
        DEM.invalidateCache()
    }

    companion object {
        private const val MAX_ZIP_FILE_COUNT = 1000
    }

}