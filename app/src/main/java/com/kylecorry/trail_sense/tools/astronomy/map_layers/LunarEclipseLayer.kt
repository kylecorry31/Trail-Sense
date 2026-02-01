package com.kylecorry.trail_sense.tools.astronomy.map_layers

import android.os.Bundle
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import java.time.Duration

class LunarEclipseLayer : TileMapLayer<LunarEclipseTileSource>(LunarEclipseTileSource()) {

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

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        source.showPath = preferences.getBoolean(SHOW_PATH, DEFAULT_SHOW_PATH)
    }

    companion object {
        const val LAYER_ID = "lunar_eclipse"
        const val SHOW_PATH = "show_path"
        const val DEFAULT_SHOW_PATH = false
        private val REFRESH_INTERVAL: Duration = Duration.ofMinutes(2)
    }
}
