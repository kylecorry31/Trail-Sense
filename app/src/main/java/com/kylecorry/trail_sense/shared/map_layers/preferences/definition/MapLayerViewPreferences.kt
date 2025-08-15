package com.kylecorry.trail_sense.shared.map_layers.preferences.definition

data class MapLayerViewPreferences(
    val layerId: String,
    val title: String,
    val preferences: List<MapLayerViewPreference>
)