package com.kylecorry.trail_sense.tools.maps.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapService

class MapCleanupCommand(context: Context) : CoroutineCommand {

    private val service = MapService.getInstance(context)
    private val files = FileSubsystem.getInstance(context)


    override suspend fun execute() = onIO {
        val mapFiles = service.getMapFilenames()
        val allFiles = files.list("maps").map { "maps/${it.name}" }
        val orphanedFiles = allFiles.filter { !mapFiles.contains(it) }
        orphanedFiles.forEach {
            files.delete(it)
        }
    }
}