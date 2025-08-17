package com.kylecorry.trail_sense.tools.beacons.map_layers

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences

class BeaconMapLayerPreferences(
    context: Context,
    mapId: String
) : BaseMapLayerPreferences(context, mapId, "beacon", R.string.beacons)