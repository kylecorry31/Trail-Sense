package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import android.content.Context
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.infrastructure.PathLoader
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.ui.asMappable
import com.kylecorry.trail_sense.tools.navigation.ui.layers.PathLayer
import com.kylecorry.trail_sense.shared.extensions.onDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PathLayerManager(private val context: Context, private val layer: PathLayer) :
    BaseLayerManager() {

    private val pathService = PathService.getInstance(context)
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
            loadRunner.replace {
                loadPaths(true)
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
        if (reload || !loaded) {
            val bounds = bounds ?: return@onDefault
            // TODO: Make unload bounds larger than load bounds
            pathLoader.update(paths, bounds, bounds, true)
            loaded = true
        }

        val points = pathLoader.getPointsWithBacktrack(context)
        onPathsChanged(paths, points)
    }

    private fun onPathsChanged(paths: List<Path>, points: Map<Long, List<PathPoint>>) {
        val mappablePaths = points.mapNotNull {
            val path =
                paths.firstOrNull { p -> p.id == it.key } ?: return@mapNotNull null
            it.value.asMappable(context, path)
        }
        layer.setPaths(mappablePaths)
    }
}