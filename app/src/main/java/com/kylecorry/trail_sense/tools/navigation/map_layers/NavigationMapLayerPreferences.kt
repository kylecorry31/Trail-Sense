package com.kylecorry.trail_sense.tools.navigation.map_layers

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerPreferenceConfig
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SeekbarMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SwitchMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences

class NavigationMapLayerPreferences(
    context: Context,
    mapId: String
) : BaseMapLayerPreferences(context, mapId, "navigation", R.string.navigation)
