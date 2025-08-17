package com.kylecorry.trail_sense.tools.map.map_layers

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences

class MyLocationMapLayerPreferences(
    context: Context,
    mapId: String
) : BaseMapLayerPreferences(context, mapId, "my_location", R.string.my_location)