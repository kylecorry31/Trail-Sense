package com.kylecorry.trail_sense.tools.tides.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayerManager

class TideMapLayerManager(private val layer: TideMapLayer) : BaseLayerManager() {

    override fun start() {
        layer.start()
    }

    override fun stop() {
        layer.stop()
    }
}