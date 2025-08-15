package com.kylecorry.trail_sense.tools.tides.map_layers

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerPreferenceConfig
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SeekbarMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SwitchMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences

class TideMapLayerPreferences(
    context: Context,
    mapId: String
) : BaseMapLayerPreferences(context) {

    private var _isEnabled by BooleanPreference(
        cache,
        "pref_${mapId}_tide_layer_enabled",
        true
    )

    val isEnabled = MapLayerPreferenceConfig(
        get = { _isEnabled },
        set = { _isEnabled = it },
        preference = SwitchMapLayerPreference(
            context.getString(R.string.visible),
            "tide_layer_enabled",
            defaultValue = true
        )
    )

    private var _opacity by IntPreference(
        cache,
        "pref_${mapId}_tide_layer_opacity",
        100 // percent
    )

    val opacity = MapLayerPreferenceConfig(
        get = { _opacity },
        set = { _opacity = it },
        preference = SeekbarMapLayerPreference(
            context.getString(R.string.opacity),
            "tide_layer_opacity",
            defaultValue = 100,
            dependency = "tide_layer_enabled"
        )
    )

    override fun getPreferences(): MapLayerViewPreferences {
        return MapLayerViewPreferences(
            "tide_layer",
            context.getString(R.string.tides),
            listOf(
                isEnabled.preference,
                opacity.preference
            )
        )
    }
}