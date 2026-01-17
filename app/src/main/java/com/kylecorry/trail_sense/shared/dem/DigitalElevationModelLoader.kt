package com.kylecorry.trail_sense.shared.dem

import android.net.Uri
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.files.ZipUtils
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.unzip
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.withLock

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

class CompressedDigitalElevationModelIndex(
    val r: Int,
    val c: String,
    val v: String?,
    val w: Int,
    val h: Int,
    val f: List<List<Number>>
) : ProguardIgnore {
    fun toDigitalElevationModelIndex(): DigitalElevationModelIndex {
        return DigitalElevationModelIndex(
            r,
            c,
            v,
            f.map {
                DigitalElevationModelFile(
                    "${it[0].toInt()}.webp",
                    if (it.size > 7) it[7].toInt() else w,
                    if (it.size > 7) it[8].toInt() else h,
                    it[1].toDouble(),
                    it[2].toDouble(),
                    it[4].toDouble(),
                    it[6].toDouble(),
                    it[3].toDouble(),
                    it[5].toDouble()
                )
            }
        )
    }
}

class DigitalElevationModelLoader {

    fun load(source: Uri): Flow<Float> = flow {
        val files = AppServiceRegistry.get<FileSubsystem>()
        val database = AppServiceRegistry.get<AppDatabase>().digitalElevationModelDao()
        val prefs = AppServiceRegistry.get<UserPreferences>()
        // Validate the source URI
        val fileCount = files.stream(source)?.use {
            val files = ZipUtils.list(it, MAX_ZIP_FILE_COUNT)
            if (!files.any { it.file.name == "index.json" }) {
                throw IllegalArgumentException("The provided zip file does not contain a valid DEM index.json file.")
            }
            files.size
        } ?: 0

        // Clear the existing files
        DEMRepo.lock.withLock {
            files.getDirectory("dem", create = true).deleteRecursively()

            // Unzip the files
            var count = 0
            files.stream(source)?.use {
                ZipUtils.unzip(it, files.getDirectory("dem", create = true), MAX_ZIP_FILE_COUNT) {
                    count++
                    emit(count / fileCount.coerceAtLeast(1).toFloat())
                }
            } ?: return@flow

            // Read the index file
            val indexFile = files.get("dem/index.json")

            // Clear tiles
            database.deleteAll()
            database.upsert(getTilesFromIndex(files.get("dem/index.json").readText()))

            // Record version
            files.get("dem/version.txt", create = true).writeText(database.getVersion() ?: "")

            // Delete the index.json file since it is no longer needed
            indexFile.delete()

            prefs.altimeter.isDigitalElevationModelLoaded = true
        }

        DEM.invalidateCache()
        emit(1f)
    }.flowOn(Dispatchers.IO)

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


        fun getTilesFromIndex(
            indexText: String,
            isCompressed: Boolean = false
        ): List<DigitalElevationModelEntity> {
            val parsed = if (isCompressed) {
                JsonConvert.fromJson<CompressedDigitalElevationModelIndex>(indexText)
                    ?.toDigitalElevationModelIndex()
                    ?: throw IllegalArgumentException("The provided zip file does not contain a valid compressed DEM index.json file.")
            } else {
                JsonConvert.fromJson<DigitalElevationModelIndex>(indexText)
                    ?: throw IllegalArgumentException("The provided zip file does not contain a valid DEM index.json file.")
            }
            return parsed.files.map {
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
            }
        }

    }

}