package com.kylecorry.trail_sense.tools.signal_finder.map_layers

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences

class CellTowerMapLayerPreferences(
    context: Context,
    mapId: String
) : BaseMapLayerPreferences(
    context,
    mapId,
    "cell_tower",
    R.string.cell_towers,
    enabledByDefault = false
)