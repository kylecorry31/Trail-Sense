package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import com.kylecorry.trail_sense.navigation.infrastructure.IPathService
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.paths.Path2
import com.kylecorry.trail_sense.shared.paths.PathMetadata
import com.kylecorry.trail_sense.shared.paths.PathStyle

class MigrateBacktrackPathsCommand(
    private val pathService: IPathService,
    private val prefs: IBacktrackPreferences
) : CoroutineCommand {

    override suspend fun execute() {
        val paths = pathService.getWaypoints()

        val style = PathStyle(
            prefs.backtrackPathStyle,
            prefs.backtrackPointStyle,
            prefs.backtrackPathColor.color,
            true
        )

        pathService.endBacktrackPath()

        for (path in paths) {
            val newPath = pathService.addPath(Path2(0, null, style, PathMetadata.empty))
            pathService.moveWaypointsToPath(path.value.map { it.copy(pathId = 0) }, newPath)
        }
    }

}