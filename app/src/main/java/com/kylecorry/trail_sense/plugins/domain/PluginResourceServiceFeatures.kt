package com.kylecorry.trail_sense.plugins.domain

import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition

data class PluginResourceServiceFeatures(
    val weather: List<String>,
    val mapLayers: List<MapLayerDefinition>
)
