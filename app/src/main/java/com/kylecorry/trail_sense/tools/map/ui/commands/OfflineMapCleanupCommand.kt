package com.kylecorry.trail_sense.tools.map.ui.commands

import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.CoroutineValueCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.map.infrastructure.persistence.OfflineMapFileRepo

class OfflineMapCleanupCommand : CoroutineValueCommand<Boolean> {

    private val repo = getAppService<OfflineMapFileRepo>()
    private val files = getAppService<FileSubsystem>()

    override suspend fun execute(): Boolean = onIO {
        val maps = repo.getAllSync()
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
            repo.delete(it)
        }

        toDelete.isNotEmpty()
    }

    companion object {
        private const val OFFLINE_MAPS_DIRECTORY = "offline_maps"
    }
}
