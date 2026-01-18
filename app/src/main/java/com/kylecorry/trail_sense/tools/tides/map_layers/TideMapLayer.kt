package com.kylecorry.trail_sense.tools.tides.map_layers

import android.os.Bundle
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import java.time.Duration

class TideMapLayer : GeoJsonLayer<TideGeoJsonSource>(TideGeoJsonSource(), layerId = LAYER_ID) {

    private val timer = CoroutineTimer {
        invalidate()
        notifyListeners()
    }

    override fun start() {
        timer.interval(Duration.ofMinutes(10))
    }

    override fun stop() {
        super.stop()
        timer.stop()
    }

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        source.showModeledTides =
            preferences.getBoolean(SHOW_MODELED_TIDES, DEFAULT_SHOW_MODELED_TIDES)
    }

    companion object {
        const val LAYER_ID = "tide"
        const val SHOW_MODELED_TIDES = "show_modeled_tides"
        const val DEFAULT_SHOW_MODELED_TIDES = false
    }
}