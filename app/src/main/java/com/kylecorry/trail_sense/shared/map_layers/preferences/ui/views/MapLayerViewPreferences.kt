package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views

data class MapLayerViewPreferences(
    val layerId: String,
    val title: String,
    val preferences: List<MapLayerViewPreference>
)