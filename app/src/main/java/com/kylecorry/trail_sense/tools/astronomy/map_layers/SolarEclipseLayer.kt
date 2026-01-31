package com.kylecorry.trail_sense.tools.astronomy.map_layers

import android.os.Bundle
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class SolarEclipseLayer : TileMapLayer<SolarEclipseTileSource>(SolarEclipseTileSource()) {

    override val layerId: String = LAYER_ID

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        source.smooth = preferences.getBoolean(SMOOTH, DEFAULT_SMOOTH)
    }

    companion object {
        const val LAYER_ID = "solar_eclipse"
        const val SMOOTH = "smooth"
        const val DEFAULT_SMOOTH = false
    }
}
