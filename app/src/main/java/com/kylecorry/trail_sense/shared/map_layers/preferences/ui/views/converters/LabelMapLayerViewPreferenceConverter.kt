package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.converters

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getDependencyBasePreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.LabelMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.MapLayerViewPreference
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class LabelMapLayerViewPreferenceConverter : MapLayerViewPreferenceConverter {
    override fun convert(preference: MapLayerPreference, layerId: String): MapLayerViewPreference {
        return LabelMapLayerPreference(
            preference.title,
            preference.summary,
            preference.getDependencyBasePreferenceKey(layerId)
        ) { context ->
            // TODO: Find a better way to do this
            if (preference.openDemSettingsOnClick && context is MainActivity) {
                context.findNavController()
                    .navigateWithAnimation(R.id.calibrateAltimeterFragment)
            }
        }
    }
}