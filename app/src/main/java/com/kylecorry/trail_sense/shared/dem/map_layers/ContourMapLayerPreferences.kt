package com.kylecorry.trail_sense.shared.dem.map_layers

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.LabelMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerPreferenceConfig
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SeekbarMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SwitchMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class ContourMapLayerPreferences(
    context: Context,
    mapId: String,
    isEnabledByDefault: Boolean = false
) : BaseMapLayerPreferences(context) {

    private var _isEnabled by BooleanPreference(
        cache,
        "pref_${mapId}_contour_layer_enabled",
        isEnabledByDefault
    )

    val isEnabled = MapLayerPreferenceConfig(
        get = { _isEnabled },
        set = { _isEnabled = it },
        preference = SwitchMapLayerPreference(
            context.getString(R.string.visible),
            "contour_layer_enabled",
            defaultValue = isEnabledByDefault
        )
    )

    private var _opacity by IntPreference(
        cache,
        "pref_${mapId}_contour_layer_opacity",
        50 // percent
    )

    val opacity = MapLayerPreferenceConfig(
        get = { _opacity },
        set = { _opacity = it },
        preference = SeekbarMapLayerPreference(
            context.getString(R.string.opacity),
            "contour_layer_opacity",
            defaultValue = 50,
            dependency = "contour_layer_enabled"
        )
    )

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
            dependency = "contour_layer_enabled",
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
            dependency = "contour_layer_enabled"
        )
    )

    override fun getPreferences(): MapLayerViewPreferences {
        return MapLayerViewPreferences(
            "contour_layer",
            context.getString(R.string.contours),
            listOf(
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
        )
    }
}