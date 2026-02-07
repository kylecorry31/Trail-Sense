package com.kylecorry.trail_sense.tools.tides.map_layers

import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import java.time.Duration

class TideMapLayer : GeoJsonLayer<TideGeoJsonSource>(TideGeoJsonSource(), layerId = LAYER_ID) {

    override val isTimeDependent = true

    private val timer = CoroutineTimer {
        refresh()
    }

    override fun start() {
        super.start()
        timer.interval(REFRESH_INTERVAL, REFRESH_INTERVAL)
    }

    override fun stop() {
        super.stop()
        timer.stop()
    }

    companion object {
        const val LAYER_ID = "tide"
        const val SHOW_MODELED_TIDES = "show_modeled_tides"
        const val DEFAULT_SHOW_MODELED_TIDES = false
        const val SHOW_PHASE = "show_phase"
        const val DEFAULT_SHOW_PHASE = false
        private val REFRESH_INTERVAL: Duration = Duration.ofMinutes(10)
    }
}
