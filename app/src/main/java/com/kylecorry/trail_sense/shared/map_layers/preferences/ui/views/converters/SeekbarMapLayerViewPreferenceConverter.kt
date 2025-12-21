package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.converters

import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getBasePreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getDependencyBasePreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.MapLayerViewPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.SeekbarMapLayerPreference

class SeekbarMapLayerViewPreferenceConverter : MapLayerViewPreferenceConverter {
    override fun convert(preference: MapLayerPreference, layerId: String): MapLayerViewPreference {
        return SeekbarMapLayerPreference(
            preference.title?.toString() ?: "",
            preference.getBasePreferenceKey(layerId),
            (preference.defaultValue as? Int) ?: 0,
            preference.min?.toInt() ?: 0,
            preference.max?.toInt() ?: 100,
            preference.getDependencyBasePreferenceKey(layerId)
        )
    }
}