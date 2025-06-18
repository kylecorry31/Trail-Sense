package com.kylecorry.trail_sense.shared.dem

import android.net.Uri
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.files.ZipUtils
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.FileSubsystem

class DigitalElevationModelFile(
    val filename: String,
    val width: Int,
    val height: Int,
    val a: Double,
    val b: Double,
    val longitude_start: Double,
    val longitude_end: Double,
    val latitude_start: Double,
    val latitude_end: Double,
) : ProguardIgnore

class DigitalElevationModelIndex(
    val resolution_arc_seconds: Int,
    val compression_method: String,
    val version: String?,
    val files: List<DigitalElevationModelFile>
) : ProguardIgnore

class DigitalElevationModelLoader {

    suspend fun load(source: Uri) = onIO {
        val files = AppServiceRegistry.get<FileSubsystem>()
        val database = AppServiceRegistry.get<AppDatabase>().digitalElevationModelDao()
        val prefs = AppServiceRegistry.get<UserPreferences>()

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

        // Read the index file
        val indexFile = files.get("dem/index.json")
        val parsed =
            JsonConvert.fromJson<DigitalElevationModelIndex>(indexFile.readText())
                ?: throw IllegalArgumentException("The provided zip file does not contain a valid DEM index.json file.")

        // Delete the index.json file since it is no longer needed
        indexFile.delete()

        database.deleteAll()
        database.upsert(parsed.files.map {
            DigitalElevationModelEntity(
                parsed.resolution_arc_seconds,
                parsed.compression_method,
                parsed.version ?: "",
                "dem/${it.filename}",
                it.width,
                it.height,
                it.a,
                it.b,
                it.latitude_start,
                it.latitude_end,
                it.longitude_end,
                it.longitude_start
            )
        })

        prefs.altimeter.isDigitalElevationModelLoaded = true

        DEM.invalidateCache()
    }

    suspend fun clear() = onIO {
        val prefs = AppServiceRegistry.get<UserPreferences>()
        val files = AppServiceRegistry.get<FileSubsystem>()
        val database = AppServiceRegistry.get<AppDatabase>().digitalElevationModelDao()

        files.getDirectory("dem", create = true).deleteRecursively()
        database.deleteAll()
        prefs.altimeter.isDigitalElevationModelLoaded = false
        DEM.invalidateCache()
    }

    companion object {
        private const val MAX_ZIP_FILE_COUNT = 1000
    }

}