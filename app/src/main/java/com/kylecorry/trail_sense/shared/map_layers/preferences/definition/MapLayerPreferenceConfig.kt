package com.kylecorry.trail_sense.shared.map_layers.preferences.definition

data class MapLayerPreferenceConfig<T>(
    val get: () -> T,
    val set: (T) -> Unit,
    val preference: MapLayerViewPreference
)