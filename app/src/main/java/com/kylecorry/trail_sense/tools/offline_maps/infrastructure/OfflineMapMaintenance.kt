package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.isExternal
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo
import kotlinx.coroutines.sync.Mutex

internal class OfflineMapMaintenance(
    private val files: FileSubsystem,
    private val repo: MapRepo
) {

    suspend fun <T> withImportLock(block: suspend () -> T): T {
        maintenanceLock.lock()
        try {
            return block()
        } finally {
            maintenanceLock.unlock()
        }
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
        val allFiles = files.list(OFFLINE_MAPS_DIRECTORY).map { "$OFFLINE_MAPS_DIRECTORY/${it.name}" }

        // Delete files without a map
        val mapFiles = maps.flatMap { it.files.map { file -> file.path } }
        val orphanedFiles = allFiles.filter { !mapFiles.contains(it) }
        orphanedFiles.forEach {
            files.delete(it)
        }

        // Delete maps without a file
        val toDelete = maps.filter { !allFiles.contains(it.mapFile.path) }
        toDelete.forEach {
            repo.delete(it)
        }

        return toDelete.isNotEmpty()
    }

    private suspend fun cleanupPhotoMaps(): Boolean {
        val maps = repo.getPhotoMaps()
        val allFiles = files.list(PHOTO_MAPS_DIRECTORY).map { "$PHOTO_MAPS_DIRECTORY/${it.name}" }

        // Delete files without a map
        val mapFiles = maps.flatMap { it.files.map { file -> file.path } }
        val orphanedFiles = allFiles.filter { !mapFiles.contains(it) }
        orphanedFiles.forEach {
            files.delete(it)
        }

        // Delete maps without a file
        val toDelete = maps.filter { !allFiles.contains(it.imageFile.path) }

        toDelete.forEach {
            repo.delete(it)
        }

        return toDelete.isNotEmpty()
    }

    companion object {
        private val maintenanceLock = Mutex()
        private const val PHOTO_MAPS_DIRECTORY = "maps"
        private const val OFFLINE_MAPS_DIRECTORY = "offline_maps"
    }
}
