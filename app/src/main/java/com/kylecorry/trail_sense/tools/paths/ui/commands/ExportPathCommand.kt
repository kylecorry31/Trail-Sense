package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.IOService
import com.kylecorry.trail_sense.tools.paths.domain.FullPath
import com.kylecorry.trail_sense.tools.paths.domain.IPath
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathGPXConverter
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.ui.PathNameFactory
import java.time.Instant


class ExportPathCommand(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val gpxService: IOService<GPXData>,
    private val pathService: IPathService = PathService.getInstance(context)
) {

    private val pathNameFactory = PathNameFactory(context)

    fun execute(path: IPath?) {
        lifecycleOwner.inBackground(BackgroundMinimumState.Created) {
            // Load the paths and groups (without waypoints)
            val all = getPaths(path)
            val paths = all.filterIsInstance<Path>()
            val groups = all.filterIsInstance<PathGroup>().associateBy { it.id }

            if (paths.isEmpty()){
                return@inBackground
            }

            val chosenIds = if (path is Path) {
                listOf(path.id)
            } else {
                val selection = CoroutinePickers.items(
                    context,
                    context.getString(R.string.export),
                    paths.map { pathNameFactory.getName(it) },
                    List(paths.size) { it }
                ) ?: return@inBackground

                if (selection.isEmpty()){
                    return@inBackground
                }

                selection.map { paths[it].id }
            }

            // Load the waypoints and associate them with the paths
            val waypoints = pathService.getWaypoints(chosenIds)
            val pathsToExport = paths
                .filter { chosenIds.contains(it.id) }
                .map {
                    val parent = it.parentId?.let { id -> groups[id] }
                    FullPath(it, waypoints[it.id] ?: emptyList(), parent)
                }

            // Export to a GPX file
            val gpx = PathGPXConverter().toGPX(pathsToExport)
            val exportFile = "trail-sense-${Instant.now().epochSecond}.gpx"
            val success = gpxService.export(gpx, exportFile)

            // Notify the user
            onMain {
                if (success) {
                    Alerts.toast(context, context.getString(R.string.path_exported))
                } else {
                    Alerts.toast(context, context.getString(R.string.export_path_error))
                }
            }
        }
    }

    private suspend fun getPaths(path: IPath?): List<IPath> = onIO {
        if (path is Path) {
            listOf(path)
        } else {
            listOfNotNull(path) + pathService.loader().getChildren(path?.id)
        }
    }


}