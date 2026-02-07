package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources

import android.content.Context

import android.os.Bundle
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds

class ConfigurableGeoJsonSource(initialData: GeoJsonObject? = null) : GeoJsonSource {

    var data: GeoJsonObject? = initialData

    override suspend fun load(context: Context, bounds: CoordinateBounds, zoom: Int, params: Bundle): GeoJsonObject? {
        return data
    }
}
