package com.kylecorry.trail_sense.tools.paths.map_layers

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARPathLayer
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import com.kylecorry.trail_sense.tools.paths.infrastructure.PathLoader
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.ui.asMappable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AugmentedRealityPathLayerManager(
    private val context: Context,
    private val layer: ARPathLayer,
    private val shouldCorrectElevations: Boolean = false
) {

    private var bounds: CoordinateBounds? = null
    private val pathService = PathService.Companion.getInstance(context)
    private val hikingService = HikingService()
    private val pathLoader = PathLoader(pathService)
    private var paths = emptyList<Path>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val loadRunner = CoroutineQueueRunner(2, scope)
    private val listenerRunner = CoroutineQueueRunner(scope = scope)
    private var loaded = false
    private var wasBacktrackOn = false

    fun start() {
        loaded = false
        scope.launch {
            listenerRunner.skipIfRunning {
                pathService.getPaths().collect {
                    paths = it.filter { path -> path.style.visible }
                    wasBacktrackOn = pathService.getBacktrackPathId() != null
                    loaded = false
                    loadRunner.replace {
                        loadPaths(true)
                    }
                }
            }
        }
    }

    fun stop() {
        listenerRunner.cancel()
        loadRunner.cancel()
        scope.cancel()
    }

    fun onBoundsChanged(bounds: CoordinateBounds?) {
        this.bounds = bounds
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