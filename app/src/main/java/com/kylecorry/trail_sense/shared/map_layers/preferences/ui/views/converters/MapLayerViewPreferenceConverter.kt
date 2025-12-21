package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.converters

import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.MapLayerViewPreference

interface MapLayerViewPreferenceConverter {
    fun convert(preference: MapLayerPreference, layerId: String): MapLayerViewPreference
}