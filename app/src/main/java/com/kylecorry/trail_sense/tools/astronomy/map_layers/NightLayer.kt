package com.kylecorry.trail_sense.tools.astronomy.map_layers

import android.os.Bundle
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import java.time.Duration

class NightLayer : TileMapLayer<NightTileSource>(NightTileSource()) {

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
        source.smooth = preferences.getBoolean(SMOOTH, DEFAULT_SMOOTH)
    }

    companion object {
        const val LAYER_ID = "night"
        const val SMOOTH = "smooth"
        const val DEFAULT_SMOOTH = false
        private val REFRESH_INTERVAL: Duration = Duration.ofMinutes(1)
    }
}
