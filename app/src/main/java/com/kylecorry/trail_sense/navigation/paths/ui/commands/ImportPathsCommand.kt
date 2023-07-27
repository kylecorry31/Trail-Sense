package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.filterIndices
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.IPathService
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathMetadata
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.domain.PathSimplificationQuality
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.IPathPreferences
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.ImportService
import com.kylecorry.trail_sense.shared.withLoading


class ImportPathsCommand(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val gpxService: ImportService<GPXData>,
    private val pathService: IPathService = PathService.getInstance(context),
    private val prefs: IPathPreferences = UserPreferences(context).navigation
) {

    fun execute(parentId: Long?) {
        lifecycleOwner.inBackground(BackgroundMinimumState.Created) {
            val gpx = gpxService.import() ?: return@inBackground
            val paths = mutableListOf<Pair<String?, List<PathPoint>>>()

            // Get the tracks and routes from the GPX
            paths.addAll(getTracks(gpx))
            paths.addAll(getRoutes(gpx))

            // Let the user select the paths to import
            val selectedPathIndices = CoroutinePickers.items(
                context,
                context.getString(R.string.import_btn),
                paths.map {
                    it.first ?: context.getString(android.R.string.untitled)
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

    private suspend fun getRoutes(gpx: GPXData): List<Pair<String?, List<PathPoint>>> = onDefault {
        val paths = mutableListOf<Pair<String?, List<PathPoint>>>()
        for (route in gpx.routes) {
            paths.add(route.name to route.points.map {
                PathPoint(
                    0, 0, it.coordinate, it.elevation, it.time
                )
            })
        }
        paths
    }

    private suspend fun getTracks(gpx: GPXData): List<Pair<String?, List<PathPoint>>> = onDefault {
        val paths = mutableListOf<Pair<String?, List<PathPoint>>>()
        for (track in gpx.tracks) {
            for ((points) in track.segments) {
                paths.add(track.name to points.map {
                    PathPoint(
                        0, 0, it.coordinate, it.elevation, it.time
                    )
                })
            }
        }
        paths
    }

    private suspend fun importPaths(
        paths: List<Pair<String?, List<PathPoint>>>, parentId: Long?
    ) = onIO {
        val shouldSimplify = prefs.simplifyPathOnImport
        val style = prefs.defaultPathStyle
        for ((name, waypoints) in paths) {
            // Create the path
            val pathToCreate = Path(0, name, style, PathMetadata.empty, parentId = parentId)
            val pathId = pathService.addPath(pathToCreate)

            // Add the waypoints to the path
            pathService.addWaypointsToPath(waypoints, pathId)

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