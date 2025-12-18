package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapMapLayerPreferences

class BaseMapLayer : TileMapLayer<BaseMapTileSource>(BaseMapTileSource()) {

    fun setPreferences(prefs: BaseMapMapLayerPreferences) {
        percentOpacity = prefs.opacity.get() / 100f
        invalidate()
    }
}