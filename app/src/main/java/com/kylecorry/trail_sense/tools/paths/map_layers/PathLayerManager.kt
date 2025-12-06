package com.kylecorry.trail_sense.tools.paths.map_layers

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayerManager
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PathLayerManager(
    private val layer: PathLayer
) :
    BaseLayerManager() {

    private val pathService = AppServiceRegistry.get<PathService>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val listenerRunner = CoroutineQueueRunner(scope = scope)
    private var wasBacktrackOn = false


    override fun start() {
        scope.launch {
            listenerRunner.skipIfRunning {
                pathService.getPaths().collect {
                    wasBacktrackOn = pathService.getBacktrackPathId() != null
                    // Paths changed, so we need to reload the paths
                    layer.reload()
                }
            }
        }
    }

    override fun stop() {
        listenerRunner.cancel()
        scope.cancel()
    }


    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        super.onLocationChanged(location, accuracy)
        if (!wasBacktrackOn) {
            return
        }
        // Location changed while backtrack is on, so we need to update the current location
        layer.invalidate()
    }
}