package com.kylecorry.trail_sense.tools.tides.map_layers

import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import java.time.Duration

class TideMapLayer : GeoJsonLayer<TideGeoJsonSource>(TideGeoJsonSource()) {

    private val timer = CoroutineTimer {
        invalidate()
    }

    override fun start() {
        timer.interval(Duration.ofMinutes(1))
    }

    override fun stop() {
        timer.stop()
    }

    fun setPreferences(prefs: TideMapLayerPreferences) {
        percentOpacity = prefs.opacity.get() / 100f
    }
}