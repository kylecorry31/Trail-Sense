package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer

interface IGeoJsonFeatureRenderer: IAsyncLayer {
    fun setFeatures(features: List<GeoJsonFeature>)
}