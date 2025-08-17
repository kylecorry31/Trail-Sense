package com.kylecorry.trail_sense.tools.map.map_layers

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences

class BaseMapMapLayerPreferences(
    context: Context,
    mapId: String
) : BaseMapLayerPreferences(context, mapId, "base_map", R.string.basemap)