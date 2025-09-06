package com.kylecorry.trail_sense.shared.dem.map_layers

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.LabelMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class ElevationMapLayerPreferences(
    context: Context,
    mapId: String,
    isEnabledByDefault: Boolean = false
) : BaseMapLayerPreferences(
    context,
    mapId,
    "elevation",
    R.string.elevation,
    isEnabledByDefault,
    50
) {

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
            opacity.preference
        )
    }
}