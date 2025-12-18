package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features

import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.IGeoJsonRenderer

interface IGeoJsonFeatureRenderer : IGeoJsonRenderer {
    fun setFeatures(features: List<GeoJsonFeature>)
}