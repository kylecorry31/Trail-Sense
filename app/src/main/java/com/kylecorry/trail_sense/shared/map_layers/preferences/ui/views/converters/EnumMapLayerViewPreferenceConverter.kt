package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.converters

import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getBasePreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getDependencyBasePreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.ListMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.MapLayerViewPreference

class EnumMapLayerViewPreferenceConverter : MapLayerViewPreferenceConverter {
    override fun convert(preference: MapLayerPreference, layerId: String): MapLayerViewPreference {
        return ListMapLayerPreference(
            preference.title?.toString() ?: "",
            preference.getBasePreferenceKey(layerId),
            preference.values ?: emptyList(),
            preference.defaultValue as? String?,
            preference.getDependencyBasePreferenceKey(layerId)
        )
    }
}