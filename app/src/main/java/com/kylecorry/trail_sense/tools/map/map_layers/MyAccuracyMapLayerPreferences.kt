package com.kylecorry.trail_sense.tools.map.map_layers

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences

class MyAccuracyMapLayerPreferences(
    context: Context,
    mapId: String
) : BaseMapLayerPreferences(
    context,
    mapId,
    "my_accuracy",
    R.string.gps_location_accuracy,
    defaultOpacityPercent = 10
)
