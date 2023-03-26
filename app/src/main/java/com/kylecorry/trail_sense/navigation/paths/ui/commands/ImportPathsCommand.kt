package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.filterIndices
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.*
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.IPathPreferences
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.ImportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ImportPathsCommand(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val gpxService: ImportService<GPXData>,
    private val pathService: IPathService = PathService.getInstance(context),
    private val prefs: IPathPreferences = UserPreferences(context).navigation
) {

    fun execute(parentId: Long?) {
        lifecycleOwner.inBackground {
            val gpx = gpxService.import() ?: return@inBackground
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
                        lifecycleOwner.inBackground {
                            val loading = withContext(Dispatchers.Main) {
                                Alerts.loading(context, context.getString(R.string.importing))
                            }

                            withContext(Dispatchers.IO) {
                                val shouldSimplify = prefs.simplifyPathOnImport
                                for (path in paths.filterIndices(it)) {
                                    val pathToCreate =
                                        Path(0, path.first, style, PathMetadata.empty, parentId = parentId)
                                    val pathId = pathService.addPath(pathToCreate)
                                    pathService.addWaypointsToPath(path.second, pathId)
                                    if (shouldSimplify) {
                                        pathService.simplifyPath(
                                            pathId,
                                            PathSimplificationQuality.High
                                        )
                                    }
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