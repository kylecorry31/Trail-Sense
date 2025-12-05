package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import com.kylecorry.andromeda.geojson.GeoJsonFeature

interface IGeoJsonFeatureRenderer : IGeoJsonRenderer {
    fun setFeatures(features: List<GeoJsonFeature>)
}