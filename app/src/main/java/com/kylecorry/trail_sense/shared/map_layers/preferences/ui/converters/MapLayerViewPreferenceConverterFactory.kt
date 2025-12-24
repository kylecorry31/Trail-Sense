package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.converters

import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceType

class MapLayerViewPreferenceConverterFactory {
    fun getConverter(preferenceType: MapLayerPreferenceType): MapLayerViewPreferenceConverter {
        return when (preferenceType) {
            MapLayerPreferenceType.Label -> LabelMapLayerViewPreferenceConverter()
            MapLayerPreferenceType.Enum -> EnumMapLayerViewPreferenceConverter()
            MapLayerPreferenceType.Seekbar -> SeekbarMapLayerViewPreferenceConverter()
            MapLayerPreferenceType.Switch -> SwitchMapLayerViewPreferenceConverter()
        }
    }
}