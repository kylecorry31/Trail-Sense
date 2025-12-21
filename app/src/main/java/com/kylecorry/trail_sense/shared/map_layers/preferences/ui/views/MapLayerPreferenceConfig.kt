package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views

data class MapLayerPreferenceConfig<T>(
    val get: () -> T,
    val set: (T) -> Unit,
    val preference: MapLayerViewPreference
)