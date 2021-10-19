package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import com.kylecorry.trail_sense.navigation.infrastructure.IPathService
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.paths.Path2
import com.kylecorry.trail_sense.shared.paths.PathMetadata

class MigrateBacktrackPathsCommand(
    private val pathService: IPathService,
    private val prefs: IPathPreferences
) : CoroutineCommand {

    override suspend fun execute() {
        val paths = pathService.getWaypoints()

        val style = prefs.defaultPathStyle

        pathService.endBacktrackPath()

        for (path in paths) {
            val newPath = pathService.addPath(Path2(0, null, style, PathMetadata.empty, temporary = true))
            pathService.moveWaypointsToPath(path.value.map { it.copy(pathId = 0) }, newPath)
        }
    }

}