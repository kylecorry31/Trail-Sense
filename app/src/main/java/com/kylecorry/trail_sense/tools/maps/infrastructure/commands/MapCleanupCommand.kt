package com.kylecorry.trail_sense.tools.maps.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.commands.CoroutineValueCommand
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapService

class MapCleanupCommand(context: Context) : CoroutineValueCommand<Boolean> {

    private val service = MapService.getInstance(context)
    private val files = FileSubsystem.getInstance(context)


    override suspend fun execute(): Boolean = onIO {
        val maps = service.getAllMaps()
        val allFiles = files.list("maps").map { "maps/${it.name}" }

        // Delete files without a map
        val mapFiles = maps.map { it.filename }
        val orphanedFiles = allFiles.filter { !mapFiles.contains(it) }
        orphanedFiles.forEach {
            files.delete(it)
        }

        // Delete maps without a file
        val toDelete = maps.filter { !allFiles.contains(it.filename) }

        toDelete.forEach {
            service.delete(it)
        }

        toDelete.isNotEmpty()
    }
}