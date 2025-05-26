package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import android.content.Context
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MapLayerManager(private val context: Context, private val layer: MapLayer) :
    BaseLayerManager() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val runner = CoroutineQueueRunner(scope = scope)

    override fun start() {
        scope.launch {
            runner.skipIfRunning {
                val repo = MapRepo.getInstance(context)
                layer.setMaps(repo.getAllMaps())
            }
        }
    }

    override fun stop() {
        runner.cancel()
        scope.cancel()
    }

    override fun onBoundsChanged(bounds: CoordinateBounds?) {
        super.onBoundsChanged(bounds)
        layer.setBounds(bounds)
    }
}