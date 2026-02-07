package com.kylecorry.trail_sense.tools.astronomy.map_layers

import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import java.time.Duration

class LunarEclipseLayer : TileMapLayer<LunarEclipseTileSource>(LunarEclipseTileSource()) {

    override val isTimeDependent = true

    override val layerId: String = LunarEclipseTileSource.SOURCE_ID

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
        private val REFRESH_INTERVAL: Duration = Duration.ofMinutes(2)
    }
}
