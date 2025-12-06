package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.ConfigurableGeoJsonSource

class ConfigurableGeoJsonLayer(initialData: GeoJsonObject = GeoJsonFeatureCollection(emptyList())) :
    GeoJsonLayer<ConfigurableGeoJsonSource>(ConfigurableGeoJsonSource(initialData)) {

    private var onClickListener: ((GeoJsonFeature) -> Boolean)? = null

    fun setData(data: GeoJsonObject) {
        source.data = data
        invalidate()
    }

    fun setOnClickListener(listener: (GeoJsonFeature) -> Boolean) {
        onClickListener = listener
    }

    override fun onClick(feature: GeoJsonFeature): Boolean {
        return onClickListener?.invoke(feature) ?: super.onClick(feature)
    }
}