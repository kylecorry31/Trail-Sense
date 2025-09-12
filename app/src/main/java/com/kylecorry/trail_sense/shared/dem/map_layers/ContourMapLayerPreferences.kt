package com.kylecorry.trail_sense.shared.dem.map_layers

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorStrategy
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.LabelMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.ListMapLayerPreference
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

    private var _colorStrategy by StringEnumPreference(
        cache,
        "pref_${mapId}_${layerId}_layer_color",
        ElevationColorStrategy.entries.associateBy { it.id.toString() },
        ElevationColorStrategy.Brown
    )

    val colorStrategy = MapLayerPreferenceConfig(
        get = { _colorStrategy },
        set = { _colorStrategy = it },
        preference = ListMapLayerPreference(
            context.getString(R.string.color),
            "${layerId}_layer_color",
            listOf(
                context.getString(R.string.color_black) to ElevationColorStrategy.Black.id.toString(),
                context.getString(R.string.color_brown) to ElevationColorStrategy.Brown.id.toString(),
                context.getString(R.string.color_gray) to ElevationColorStrategy.Gray.id.toString(),
                context.getString(R.string.color_white) to ElevationColorStrategy.White.id.toString(),
                context.getString(R.string.color_usgs) to ElevationColorStrategy.USGS.id.toString(),
                context.getString(R.string.color_grayscale) to ElevationColorStrategy.Grayscale.id.toString(),
                context.getString(R.string.color_muted) to ElevationColorStrategy.Muted.id.toString(),
                context.getString(R.string.color_vibrant) to ElevationColorStrategy.Vibrant.id.toString(),
                context.getString(R.string.color_viridis) to ElevationColorStrategy.Viridis.id.toString(),
                context.getString(R.string.color_inferno) to ElevationColorStrategy.Inferno.id.toString(),
                context.getString(R.string.color_plasma) to ElevationColorStrategy.Plasma.id.toString(),
            ),
            dependency = enabledPreferenceId,
            defaultValue = ElevationColorStrategy.Brown.id.toString()
        )
    )

    override fun getAllPreferences(): List<MapLayerViewPreference> {
        return listOf(
            isEnabled.preference,
            LabelMapLayerPreference(
                context.getString(R.string.plugin_digital_elevation_model),
                context.getString(R.string.open_settings)
            ) { ctx ->
                if (ctx is MainActivity) {
                    ctx.findNavController()
                        .navigateWithAnimation(R.id.calibrateAltimeterFragment)
                }
            },
            opacity.preference,
            showLabels.preference,
            colorStrategy.preference
        )
    }
}