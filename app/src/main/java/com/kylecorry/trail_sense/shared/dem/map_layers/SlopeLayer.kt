package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import com.kylecorry.trail_sense.shared.dem.colors.SlopeColorStrategy

class SlopeLayer : TileMapLayer<SlopeMapTileSource>(SlopeMapTileSource(), minZoomLevel = 10) {

    override val layerId: String = LAYER_ID

    companion object {
        const val LAYER_ID = "slope"
        const val COLOR = "color"
        const val HIGH_RESOLUTION = "high_resolution"
        const val SMOOTH = "smooth"
        const val HIDE_FLAT_GROUND = "hide_flat_ground"
        val DEFAULT_COLOR = SlopeColorStrategy.GreenToRed
        const val DEFAULT_HIGH_RESOLUTION = false
        const val DEFAULT_SMOOTH = true
        const val DEFAULT_HIDE_FLAT_GROUND = false
    }
}
