package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources

import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds

class CachedGeoJsonSource(private val loader: suspend () -> GeoJsonObject?) : GeoJsonSource {
    private var cached: GeoJsonObject? = null
    private var isLoaded = false

    override suspend fun load(
        bounds: CoordinateBounds,
        zoom: Int
    ): GeoJsonObject? {
        if (!isLoaded) {
            cached = loader()
            isLoaded = true
        }
        return cached
    }
}