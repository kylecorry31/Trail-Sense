package com.kylecorry.trail_sense.tools.map.map_layers

import com.kylecorry.luna.coroutines.BackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class BaseMapLayer : TileMapLayer<BaseMapTileSource>(BaseMapTileSource()) {
    override val layerId: String = LAYER_ID

    private val recycleTask = BackgroundTask {
        source.recycle()
    }

    override fun stop() {
        super.stop()
        recycleTask.start()
    }

    companion object {
        const val LAYER_ID = "base_map"
    }
}