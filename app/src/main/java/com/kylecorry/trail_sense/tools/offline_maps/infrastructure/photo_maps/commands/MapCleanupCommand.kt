package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.shared.commands.CoroutineValueCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService

class MapCleanupCommand(context: Context) : CoroutineValueCommand<Boolean> {

    private val service = MapService.getInstance(context)
    private val files = FileSubsystem.getInstance(context)


    override suspend fun execute(): Boolean = onIO {
        if (isMapImportInProgress) {
            return@onIO false
        }

        val didDeletePhotoMaps = cleanupPhotoMaps()
        val didDeleteVectorMaps = cleanupVectorMaps()
        didDeletePhotoMaps || didDeleteVectorMaps
    }

    private suspend fun cleanupVectorMaps(): Boolean {
        val maps = service.getAllVectorMaps()
        val allFiles = files.list(OFFLINE_MAPS_DIRECTORY).map { "$OFFLINE_MAPS_DIRECTORY/${it.name}" }

        // Delete files without a map
        val mapFiles = maps.map { it.path }
        val orphanedFiles = allFiles.filter { !mapFiles.contains(it) }
        orphanedFiles.forEach {
            files.delete(it)
        }

        // Delete maps without a file
        val toDelete = maps.filter { !allFiles.contains(it.path) }
        toDelete.forEach {
            service.delete(it)
        }

        return toDelete.isNotEmpty()
    }

    private suspend fun cleanupPhotoMaps(): Boolean {
        val maps = service.getAllPhotoMaps()
        val allFiles = files.list("maps").map { "maps/${it.name}" }

        // Delete files without a map
        val mapFiles = maps.flatMap { listOf(it.filename, it.pdfFileName) }
        val orphanedFiles = allFiles.filter { !mapFiles.contains(it) }
        orphanedFiles.forEach {
            files.delete(it)
        }

        // Delete maps without a file
        val toDelete = maps.filter { !allFiles.contains(it.filename) }

        toDelete.forEach {
            service.delete(it)
        }

        return toDelete.isNotEmpty()
    }

    companion object {
        @Volatile
        var isMapImportInProgress = false

        private const val OFFLINE_MAPS_DIRECTORY = "offline_maps"
    }
}
