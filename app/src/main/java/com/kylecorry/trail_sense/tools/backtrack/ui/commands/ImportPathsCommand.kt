package com.kylecorry.trail_sense.tools.backtrack.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.filterIndices
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.infrastructure.IPathService
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.IPathPreferences
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.ImportService
import com.kylecorry.trail_sense.shared.paths.Path
import com.kylecorry.trail_sense.shared.paths.PathMetadata
import com.kylecorry.trail_sense.shared.paths.PathPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ImportPathsCommand(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val gpxService: ImportService<GPXData>,
    private val pathService: IPathService = PathService.getInstance(context),
    private val prefs: IPathPreferences = UserPreferences(context).navigation
) {

    fun execute() {
        lifecycleScope.launch {
            val gpx = gpxService.import() ?: return@launch
            val style = prefs.defaultPathStyle
            val paths = mutableListOf<Pair<String?, List<PathPoint>>>()
            for (track in gpx.tracks) {
                for (segment in track.segments) {
                    paths.add(track.name to segment.points.map {
                        PathPoint(
                            0,
                            0,
                            it.coordinate,
                            it.elevation,
                            it.time
                        )
                    })
                }
            }

            withContext(Dispatchers.Main) {
                Pickers.items(context, context.getString(R.string.import_btn),
                    paths.map {
                        it.first ?: context.getString(android.R.string.untitled)
                    },
                    List(paths.size) { it }
                ) {
                    if (it != null) {
                        lifecycleScope.launch {
                            val loading = withContext(Dispatchers.Main) {
                                Alerts.loading(context, context.getString(R.string.importing))
                            }

                            withContext(Dispatchers.IO) {
                                for (path in paths.filterIndices(it)) {
                                    val pathToCreate =
                                        Path(0, path.first, style, PathMetadata.empty)
                                    val pathId = pathService.addPath(pathToCreate)
                                    pathService.addWaypointsToPath(path.second, pathId)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Alerts.toast(
                                    context,
                                    context.resources.getQuantityString(
                                        R.plurals.paths_imported,
                                        it.size,
                                        it.size
                                    )
                                )
                                loading.dismiss()
                            }
                        }
                    }
                }
            }
        }
    }
}