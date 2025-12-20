package com.kylecorry.trail_sense.tools.map.map_layers

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerPreferenceConfig
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SwitchMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences

class MyLocationMapLayerPreferences(
    context: Context,
    mapId: String
) : BaseMapLayerPreferences(context, mapId, "my_location", R.string.my_location) {

    private var _showAccuracy by BooleanPreference(
        cache,
        "pref_${mapId}_my_location_layer_show_accuracy",
        true
    )

    val showAccuracy = MapLayerPreferenceConfig(
        get = { _showAccuracy },
        set = { _showAccuracy = it },
        preference = SwitchMapLayerPreference(
            context.getString(R.string.show_gps_accuracy),
            "my_location_layer_show_accuracy",
            dependency = enabledPreferenceId,
            defaultValue = true
        )
    )

    override fun getAllPreferences(): List<MapLayerViewPreference> {
        return super.getAllPreferences() + listOf(
            showAccuracy.preference
        )
    }

    companion object {
        const val SHOW_ACCURACY = "showAccuracy"
    }
}