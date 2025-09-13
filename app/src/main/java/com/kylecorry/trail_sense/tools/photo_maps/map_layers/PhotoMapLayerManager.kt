package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import android.content.Context
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.TiledMapLayer
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapTileSourceSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PhotoMapLayerManager(
    private val context: Context,
    private val layer: TiledMapLayer,
    private val loadPdfs: Boolean = true,
    private val mapFilter: (PhotoMap) -> Boolean = { it.visible }
) :
    BaseLayerManager() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val runner = CoroutineQueueRunner(scope = scope)

    override fun start() {
        scope.launch {
            runner.skipIfRunning {
                val repo = MapRepo.Companion.getInstance(context)
                layer.sourceSelector =
                    PhotoMapTileSourceSelector(
                        context,
                        repo.getAllMaps().filter(mapFilter),
                        8,
                        loadPdfs
                    )
            }
        }
    }

    override fun stop() {
        runner.cancel()
        scope.cancel()
    }
}