package com.kylecorry.trail_sense.tools.tools.infrastructure

import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayerPreferenceManager

data class ToolMap(
    val mapId: String,
    val layerPreferences: List<BaseMapLayerPreferences>
) {
    val manager = MapLayerPreferenceManager(
        mapId,
        layerPreferences
    )
}