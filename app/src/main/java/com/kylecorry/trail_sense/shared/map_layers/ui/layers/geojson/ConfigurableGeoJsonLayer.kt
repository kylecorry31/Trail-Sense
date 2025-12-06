package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.ConfigurableGeoJsonSource

class ConfigurableGeoJsonLayer(initialData: GeoJsonObject) :
    GeoJsonLayer<ConfigurableGeoJsonSource>(ConfigurableGeoJsonSource(initialData)) {
    fun setData(data: GeoJsonObject) {
        source.data = data
        invalidate()
    }
}