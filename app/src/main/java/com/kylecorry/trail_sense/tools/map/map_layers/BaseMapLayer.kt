package com.kylecorry.trail_sense.tools.map.map_layers

import android.os.Bundle
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class BaseMapLayer : TileMapLayer<BaseMapTileSource>(BaseMapTileSource()) {

    override fun setPreferences(preferences: Bundle) {
        percentOpacity = preferences.getInt(BaseMapLayerPreferences.OPACITY) / 100f
    }
}