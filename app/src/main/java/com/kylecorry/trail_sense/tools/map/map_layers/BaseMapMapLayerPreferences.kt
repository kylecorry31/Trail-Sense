package com.kylecorry.trail_sense.tools.map.map_layers

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerPreferenceConfig
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SeekbarMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SwitchMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences

class BaseMapMapLayerPreferences(
    context: Context,
    mapId: String,
    enabledByDefault: Boolean = true,
    defaultOpacity: Int = 100
) : BaseMapLayerPreferences(context) {

    private var _isEnabled by BooleanPreference(
        cache,
        "pref_${mapId}_base_map_layer_enabled",
        enabledByDefault
    )

    val isEnabled = MapLayerPreferenceConfig(
        get = { _isEnabled },
        set = { _isEnabled = it },
        preference = SwitchMapLayerPreference(
            context.getString(R.string.visible),
            "base_map_layer_enabled",
            defaultValue = enabledByDefault
        )
    )

    private var _opacity by IntPreference(
        cache,
        "pref_${mapId}_base_map_layer_opacity",
        defaultOpacity // percent
    )

    val opacity = MapLayerPreferenceConfig(
        get = { _opacity },
        set = { _opacity = it },
        preference = SeekbarMapLayerPreference(
            context.getString(R.string.opacity),
            "base_map_layer_opacity",
            defaultValue = defaultOpacity,
            dependency = "base_map_layer_enabled"
        )
    )

    override fun getPreferences(): MapLayerViewPreferences {
        return MapLayerViewPreferences(
            "base_map_layer",
            context.getString(R.string.basemap),
            listOf(
                isEnabled.preference,
                opacity.preference
            )
        )
    }
}
