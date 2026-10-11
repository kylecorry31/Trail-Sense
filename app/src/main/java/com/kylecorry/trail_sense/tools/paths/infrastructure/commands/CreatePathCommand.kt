package com.kylecorry.trail_sense.tools.paths.infrastructure.commands

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineValueCommand
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.infrastructure.PathElevationPrefetcher
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathPreferences

class CreatePathCommand(
    private val pathService: IPathService,
    private val prefs: IPathPreferences,
    private val name: String?,
    private val knownElevations: Map<Coordinate, Float> = emptyMap(),
    private val shouldSkipElevations: () -> Boolean = { false }
) : CoroutineValueCommand<List<Coordinate>, Long> {
    override suspend fun execute(value: List<Coordinate>) = onIO {
        val newPath = Path(
            0,
            name,
            prefs.defaultPathStyle,
            PathMetadata.empty
        )

        val newPathId = pathService.addPath(newPath)

        val waypoints = value.filter { PathElevationPrefetcher.isValid(it) }.map {
            val elevation = knownElevations[it]
                ?: if (shouldSkipElevations()) null else DEM.getElevation(it).elevation
            PathPoint(0, newPathId, it, elevation)
        }

        pathService.addWaypointsToPath(waypoints, newPathId)
        newPathId
    }
}
