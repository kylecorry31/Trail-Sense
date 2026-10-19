package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.isExternal
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class OfflineMapMaintenance(
    private val files: FileSubsystem,
    private val repo: MapRepo
) {

    suspend fun <T> withImportLock(block: suspend () -> T): T {
        return maintenanceLock.withLock { block() }
    }

    suspend fun cleanup(): Boolean = onIO {
        if (!maintenanceLock.tryLock()) {
            return@onIO false
        }

        try {
            val didDeletePhotoMaps = cleanupPhotoMaps()
            val didDeleteTrailMaps = cleanupTrailMaps()
            didDeletePhotoMaps || didDeleteTrailMaps
        } finally {
            maintenanceLock.unlock()
        }
    }

    private suspend fun cleanupTrailMaps(): Boolean {
        // External maps don't have a file in app storage, so they can't be cleaned up
        val maps = repo.getTrailMaps().filter { !it.isExternal() }
        return cleanupDirectory(maps, OFFLINE_MAPS_DIRECTORY, { it.mapFile.path }) { repo.delete(it) }
    }

    private suspend fun cleanupPhotoMaps(): Boolean {
        val maps = repo.getPhotoMaps()
        return cleanupDirectory(maps, PHOTO_MAPS_DIRECTORY, { it.imageFile.path }) { repo.delete(it) }
    }

    /**
     * Deletes files in the directory that no map uses, and maps whose main file is missing.
     * @return true if any maps were deleted
     */
    private suspend fun <T : OfflineMap> cleanupDirectory(
        maps: List<T>,
        directory: String,
        mainFilePath: (T) -> String,
        deleteMap: suspend (T) -> Unit
    ): Boolean {
        val allFiles = files.list(directory).map { "$directory/${it.name}" }

        val mapFiles = maps.flatMap { it.files.map { file -> file.path } }
        allFiles.filter { !mapFiles.contains(it) }.forEach { files.delete(it) }

        val toDelete = maps.filter { !allFiles.contains(mainFilePath(it)) }
        toDelete.forEach { deleteMap(it) }

        return toDelete.isNotEmpty()
    }

    companion object {
        private val maintenanceLock = Mutex()
        private const val PHOTO_MAPS_DIRECTORY = "maps"
        private const val OFFLINE_MAPS_DIRECTORY = "offline_maps"
    }
}
