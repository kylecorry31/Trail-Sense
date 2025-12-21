package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.converters

import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getBasePreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getDependencyBasePreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.MapLayerViewPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.SwitchMapLayerPreference

class SwitchMapLayerViewPreferenceConverter : MapLayerViewPreferenceConverter {
    override fun convert(preference: MapLayerPreference, layerId: String): MapLayerViewPreference {
        return SwitchMapLayerPreference(
            preference.title?.toString() ?: "",
            preference.getBasePreferenceKey(layerId),
            (preference.defaultValue as? Boolean) ?: true,
            preference.getDependencyBasePreferenceKey(layerId),
            preference.summary?.toString()
        )
    }

}