package com.kylecorry.trail_sense.tools.astronomy.map_layers

import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import java.time.Duration

class SolarEclipseLayer : TileMapLayer<SolarEclipseTileSource>(SolarEclipseTileSource()) {

    override val isTimeDependent = true

    override val layerId: String = LAYER_ID

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
        const val LAYER_ID = "solar_eclipse"
        const val SMOOTH = "smooth"
        const val SHOW_PATH = "show_path"
        const val DEFAULT_SMOOTH = false
        const val DEFAULT_SHOW_PATH = false
        private val REFRESH_INTERVAL: Duration = Duration.ofMinutes(2)
    }
}
