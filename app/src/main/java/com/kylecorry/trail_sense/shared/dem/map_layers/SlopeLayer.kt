package com.kylecorry.trail_sense.shared.dem.map_layers

import android.os.Bundle
import com.kylecorry.trail_sense.shared.dem.colors.SlopeColorMapFactory
import com.kylecorry.trail_sense.shared.dem.colors.SlopeColorStrategy
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import com.kylecorry.trail_sense.shared.withId

class SlopeLayer : TileMapLayer<SlopeMapTileSource>(SlopeMapTileSource(), minZoomLevel = 10) {

    override val layerId: String = LAYER_ID

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        val strategyId = preferences.getString(COLOR)?.toLongOrNull()
        source.colorMap = SlopeColorMapFactory().getSlopeColorMap(
            SlopeColorStrategy.entries.withId(strategyId ?: 0) ?: DEFAULT_COLOR
        )
        source.highResolution = preferences.getBoolean(HIGH_RESOLUTION, DEFAULT_HIGH_RESOLUTION)
        source.smooth = preferences.getBoolean(SMOOTH, DEFAULT_SMOOTH)
    }

    companion object {
        const val LAYER_ID = "slope"
        const val COLOR = "color"
        const val HIGH_RESOLUTION = "high_resolution"
        const val SMOOTH = "smooth"
        val DEFAULT_COLOR = SlopeColorStrategy.GreenToRed
        const val DEFAULT_HIGH_RESOLUTION = false
        const val DEFAULT_SMOOTH = true
    }
}
