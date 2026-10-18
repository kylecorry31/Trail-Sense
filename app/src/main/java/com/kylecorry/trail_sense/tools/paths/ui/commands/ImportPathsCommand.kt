package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.extensions.filterIndices
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.gpx.GPXWaypoint
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.ImportService
import com.kylecorry.trail_sense.tools.paths.domain.FullPath
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathSimplificationQuality
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathPreferences
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService

class ImportPathsCommand(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val gpxService: ImportService<GPXData>,
    private val pathService: IPathService = PathService.getInstance(context),
    private val prefs: IPathPreferences = UserPreferences(context).paths
) {

    private val style = prefs.defaultPathStyle

    fun execute(parentId: Long?) {
        lifecycleOwner.inBackground(BackgroundMinimumState.Created) {
            val gpx = gpxService.import() ?: return@inBackground

            // Get the tracks and routes from the GPX
            val paths = getTracks(gpx) + getRoutes(gpx)

            // Let the user select the paths to import
            val selectedPathIndices = CoroutinePickers.items(
                context,
                context.getString(R.string.import_btn),
                paths.map {
                    it.path.name ?: context.getString(android.R.string.untitled)
                },
                paths.indices.toList()
            ) ?: return@inBackground


            Alerts.withLoading(context, context.getString(R.string.importing)) {
                // Import the selected paths
                val selectedPaths = paths.filterIndices(selectedPathIndices)
                importPaths(selectedPaths, parentId)

                // Let the user know the import was successful
                Alerts.toast(
                    context, context.resources.getQuantityString(
                        R.plurals.paths_imported,
                        selectedPathIndices.size,
                        selectedPathIndices.size
                    )
                )
            }
        }
    }

    private suspend fun getRoutes(gpx: GPXData): List<FullPath> = onDefault {
        // Groups are a Trail Sense concept, so routes don't have groups
        gpx.routes.map { route ->
            val path = Path(0, route.name, style, PathMetadata.empty)
            FullPath(path, route.points.toPathPoints())
        }
    }

    private suspend fun getTracks(gpx: GPXData): List<FullPath> = onDefault {
        gpx.tracks.flatMap { track ->
            val parent = track.group?.let { PathGroup(0, it) }
            track.segments.map { (points) ->
                val path = Path(0, track.name, style, PathMetadata.empty)
                FullPath(path, points.toPathPoints(), parent)
            }
        }
    }

    private fun List<GPXWaypoint>.toPathPoints(): List<PathPoint> {
        return map {
            PathPoint(0, 0, it.coordinate, it.elevation, it.time)
        }
    }

    private suspend fun importPaths(
        paths: List<FullPath>, parentId: Long?
    ) = onIO {
        val shouldSimplify = prefs.simplifyPathOnImport

        val groupIdMap = paths.mapNotNull { it.parent?.name }
            .distinct()
            .associateWith { pathService.addGroup(PathGroup(0, it, parentId)) }

        for (path in paths) {
            val parent = if (path.parent != null) {
                groupIdMap[path.parent.name]
            } else {
                parentId
            }
            // Create the path
            val pathToCreate = path.path.copy(parentId = parent)
            val pathId = pathService.addPath(pathToCreate)

            // Add the waypoints to the path
            pathService.addWaypointsToPath(path.points, pathId)

            // Simplify the path
            if (shouldSimplify) {
                pathService.simplifyPath(
                    pathId,
                    PathSimplificationQuality.High
                )
            }
        }
    }

}
