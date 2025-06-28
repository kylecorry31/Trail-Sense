package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers

import android.content.Context
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapTileSourceSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PhotoMapLayerManager(
    private val context: Context,
    private val layer: MapLayer,
    private val replaceWhitePixels: Boolean = false
) :
    BaseLayerManager() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val runner = CoroutineQueueRunner(scope = scope)

    override fun start() {
        scope.launch {
            runner.skipIfRunning {
                val repo = MapRepo.getInstance(context)
                layer.sourceSelector =
                    PhotoMapTileSourceSelector(repo.getAllMaps(), 8, replaceWhitePixels)
            }
        }
    }

    override fun stop() {
        runner.cancel()
        scope.cancel()
    }
}