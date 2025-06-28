package com.kylecorry.trail_sense.tools.photo_maps.ui.commands

import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineValueCommand
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathPreferences

class CreatePathCommand(
    private val pathService: IPathService,
    private val prefs: IPathPreferences,
    private val map: PhotoMap
) : CoroutineValueCommand<List<Coordinate>, Long> {
    override suspend fun execute(value: List<Coordinate>) = onIO {
        val newPath = Path(
            0,
            map.name,
            prefs.defaultPathStyle,
            PathMetadata.empty
        )

        val newPathId = pathService.addPath(newPath)

        val waypoints = value.mapNotNull {
            // TODO: Figure out why some of these are NaN
            if (it.latitude.isNaN() || it.longitude.isNaN()) {
                return@mapNotNull null
            }
            PathPoint(0, newPathId, it)
        }

        pathService.addWaypointsToPath(waypoints, newPathId)
        newPathId
    }
}