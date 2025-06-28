package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import com.kylecorry.trail_sense.tools.paths.infrastructure.PathLoader
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.ui.IPathLayer
import com.kylecorry.trail_sense.tools.paths.ui.asMappable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PathLayerManager(
    private val context: Context,
    private val layer: IPathLayer,
    private val shouldCorrectElevations: Boolean = false
) :
    BaseLayerManager() {

    private val pathService = PathService.getInstance(context)
    private val hikingService = HikingService()
    private val pathLoader = PathLoader(pathService)
    private var paths = emptyList<Path>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val loadRunner = CoroutineQueueRunner(2, scope)
    private val listenerRunner = CoroutineQueueRunner(scope = scope)
    private var loaded = false

    override fun start() {
        loaded = false
        scope.launch {
            listenerRunner.skipIfRunning {
                pathService.getPaths().collect {
                    paths = it.filter { path -> path.style.visible }
                    loaded = false
                    loadRunner.replace {
                        loadPaths(true)
                    }
                }
            }
        }
    }

    override fun stop() {
        listenerRunner.cancel()
        loadRunner.cancel()
        scope.cancel()
    }

    override fun onBoundsChanged(bounds: CoordinateBounds?) {
        super.onBoundsChanged(bounds)
        scope.launch {
            loadRunner.enqueue {
                loadPaths(false)
            }
        }
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        super.onLocationChanged(location, accuracy)
        scope.launch {
            loadRunner.enqueue {
                loadPaths(false)
            }
        }
    }

    private suspend fun loadPaths(reload: Boolean) = onDefault {
        bounds?.let {
            pathLoader.update(paths, it, it, reload || !loaded)
            loaded = true
        }

        val points = pathLoader.getPointsWithBacktrack(context)
        onPathsChanged(paths, points)
    }

    private fun onPathsChanged(paths: List<Path>, points: Map<Long, List<PathPoint>>) {
        val mappablePaths = points.mapNotNull {
            val path =
                paths.firstOrNull { p -> p.id == it.key } ?: return@mapNotNull null

            val correctedPoints = if (shouldCorrectElevations) {
                hikingService.correctElevations(it.value.sortedBy { it.id }).reversed()
            } else {
                it.value
            }

            correctedPoints.asMappable(context, path)
        }
        layer.setPaths(mappablePaths)
    }
}