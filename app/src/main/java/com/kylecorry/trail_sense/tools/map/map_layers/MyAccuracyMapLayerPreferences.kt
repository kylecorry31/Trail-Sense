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

class MyAccuracyMapLayerPreferences(
    context: Context,
    mapId: String
) : BaseMapLayerPreferences(context) {

    private var _isEnabled by BooleanPreference(
        cache,
        "pref_${mapId}_my_accuracy_layer_enabled",
        true
    )

    val isEnabled = MapLayerPreferenceConfig(
        get = { _isEnabled },
        set = { _isEnabled = it },
        preference = SwitchMapLayerPreference(
            context.getString(R.string.visible),
            "my_accuracy_layer_enabled",
            defaultValue = true
        )
    )

    private var _opacity by IntPreference(
        cache,
        "pref_${mapId}_my_accuracy_layer_opacity",
        10 // percent
    )

    val opacity = MapLayerPreferenceConfig(
        get = { _opacity },
        set = { _opacity = it },
        preference = SeekbarMapLayerPreference(
            context.getString(R.string.opacity),
            "my_accuracy_layer_opacity",
            defaultValue = 10,
            dependency = "my_accuracy_layer_enabled"
        )
    )

    override fun getPreferences(): MapLayerViewPreferences {
        return MapLayerViewPreferences(
            "my_accuracy_layer",
            context.getString(R.string.gps_location_accuracy),
            listOf(
                isEnabled.preference,
                opacity.preference
            )
        )
    }
}
