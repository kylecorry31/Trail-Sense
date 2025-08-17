package com.kylecorry.trail_sense.shared.dem.map_layers

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.LabelMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerPreferenceConfig
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SwitchMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class ContourMapLayerPreferences(
    context: Context,
    mapId: String,
    isEnabledByDefault: Boolean = false
) : BaseMapLayerPreferences(context, mapId, "contour", R.string.contours, isEnabledByDefault, 50) {

    private var _showLabels by BooleanPreference(
        cache,
        "pref_${mapId}_contour_layer_show_labels",
        true
    )

    val showLabels = MapLayerPreferenceConfig(
        get = { _showLabels },
        set = { _showLabels = it },
        preference = SwitchMapLayerPreference(
            context.getString(R.string.show_labels),
            "contour_layer_show_labels",
            dependency = enabledPreferenceId,
            defaultValue = true
        )
    )

    private var _colorWithElevation by BooleanPreference(
        cache,
        "pref_${mapId}_contour_layer_color_with_elevation",
        false
    )

    val colorWithElevation = MapLayerPreferenceConfig(
        get = { _colorWithElevation },
        set = { _colorWithElevation = it },
        preference = SwitchMapLayerPreference(
            context.getString(R.string.color_with_elevation),
            "contour_layer_color_with_elevation",
            defaultValue = false,
            dependency = enabledPreferenceId
        )
    )

    override fun getAllPreferences(): List<MapLayerViewPreference> {
        return listOf(
            isEnabled.preference,
            LabelMapLayerPreference(
                context.getString(R.string.plugin_digital_elevation_model),
                context.getString(R.string.open_settings)
            ) {
                if (context is MainActivity) {
                    context.findNavController()
                        .navigateWithAnimation(R.id.calibrateAltimeterFragment)
                }
            },
            opacity.preference,
            showLabels.preference,
            colorWithElevation.preference
        )
    }
}