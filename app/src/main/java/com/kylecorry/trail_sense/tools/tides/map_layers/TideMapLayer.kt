package com.kylecorry.trail_sense.tools.tides.map_layers

import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import java.time.Duration

class TideMapLayer : GeoJsonLayer<TideGeoJsonSource>(TideGeoJsonSource(), layerId = LAYER_ID) {

    private val timer = CoroutineTimer {
        invalidate()
    }

    override fun start() {
        timer.interval(Duration.ofMinutes(1))
    }

    override fun stop() {
        super.stop()
        timer.stop()
    }

    companion object {
        const val LAYER_ID = "tide"
    }
}